/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.feature.sbom.k8s.resources;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.runtime.OperationConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.config.TektonConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.Duration;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.fabric8.tekton.pipeline.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.pipeline.v1beta1.WorkspaceBindingBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.BulkDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@KubernetesDependent(resourceDiscriminator = OperationGenerateResourceDiscriminator.class)
@Slf4j
public class TaskRunOperationGenerateDependentResource extends KubernetesDependentResource<TaskRun, GenerationRequest>
        implements BulkDependentResource<TaskRun, GenerationRequest> {

    public static final String TASK_NAME = "sbomer-operation-generate";

    /**
     * Parameter holding the configuration for a given deliverable analysis operation.
     */
    public static final String PARAM_COMMAND_OPERATION_CONFIG_NAME = "operation-config";
    /**
     * The index of the deliverable within the configuration.
     */
    public static final String PARAM_COMMAND_DELIVERABLE_INDEX_NAME = "deliverable-index";

    @Inject
    TektonConfig tektonConfig;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    TektonClient tektonClient;

    TaskRunOperationGenerateDependentResource() {
        super(TaskRun.class);
    }

    public TaskRunOperationGenerateDependentResource(Class<TaskRun> resourceType) {
        super(TaskRun.class);
    }

    @Override
    public Map<String, TaskRun> desiredResources(GenerationRequest primary, Context<GenerationRequest> context) {
        OperationConfig config = primary.toOperationConfig();
        if (config == null) {
            throw new ApplicationException(
                    "Unable to parse configuration from GenerationRequest '{}': {}",
                    primary.getMetadata().getName(),
                    primary.getConfig());
        }

        Map<String, TaskRun> taskRuns = new HashMap<>(config.getDeliverableUrls().size());

        for (int i = 0; i < config.getDeliverableUrls().size(); i++) {
            taskRuns.put(Integer.toString(i), desired(config, i, primary, context));
        }

        return taskRuns;
    }

    private TaskRun desired(
            OperationConfig config,
            int index,
            GenerationRequest generationRequest,
            Context<GenerationRequest> context) {

        log.debug(
                "Preparing dependent resource for the '{}' phase related to '{}' GenerationRequest",
                SbomGenerationPhase.OPERATIONGENERATE,
                generationRequest.getMetadata().getName());

        Map<String, String> labels = Labels.defaultLabelsToMap(GenerationRequestType.OPERATION);

        labels.put(Labels.LABEL_IDENTIFIER, generationRequest.getIdentifier());
        labels.put(Labels.LABEL_PHASE, SbomGenerationPhase.OPERATIONGENERATE.name().toLowerCase());
        labels.put(Labels.LABEL_GENERATION_REQUEST_ID, generationRequest.getId());

        String configStr;

        try {
            configStr = ObjectMapperProvider.json().writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Could not serialize runtime configuration into YAML", e);
        }

        Duration timeout = null;

        try {
            timeout = Duration.parse("6h");
        } catch (ParseException e) {
            throw new ApplicationException("Cannot set timeout", e);
        }

        return new TaskRunBuilder().withNewMetadata()
                .withNamespace(generationRequest.getMetadata().getNamespace())
                .withLabels(labels)
                .withName(resourceName(generationRequest, index))
                .withOwnerReferences(
                        new OwnerReferenceBuilder().withKind(generationRequest.getKind())
                                .withName(generationRequest.getMetadata().getName())
                                .withApiVersion(generationRequest.getApiVersion())
                                .withUid(generationRequest.getMetadata().getUid())
                                .build())
                .endMetadata()
                .withNewSpec()
                .withServiceAccountName(tektonConfig.sa())
                .withTimeout(timeout)
                .withParams(
                        new ParamBuilder().withName(PARAM_COMMAND_OPERATION_CONFIG_NAME)
                                .withNewValue(configStr)
                                .build(),
                        new ParamBuilder().withName(PARAM_COMMAND_DELIVERABLE_INDEX_NAME)
                                .withNewValue(String.valueOf(index))
                                .build())
                .withTaskRef(new TaskRefBuilder().withName(TASK_NAME).build())
                .withWorkspaces(
                        new WorkspaceBindingBuilder().withSubPath(generationRequest.getMetadata().getName())
                                .withName("data")
                                .withPersistentVolumeClaim(
                                        new PersistentVolumeClaimVolumeSourceBuilder().withClaimName("sbomer-sboms")
                                                .build())
                                .build())
                .endSpec()
                .build();

    }

    private String resourceName(GenerationRequest generationRequest, int index) {
        return generationRequest.dependentResourceName(SbomGenerationPhase.OPERATIONGENERATE) + "-" + index;
    }

    @Override
    public Map<String, TaskRun> getSecondaryResources(
            GenerationRequest generationRequest,
            Context<GenerationRequest> context) {

        return context.getSecondaryResources(TaskRun.class)
                .stream()
                .filter(
                        tr -> tr.getMetadata()
                                .getName()
                                .startsWith(
                                        generationRequest.dependentResourceName(SbomGenerationPhase.OPERATIONGENERATE)
                                                + "-"))
                .collect(
                        Collectors.toMap(
                                tr -> tr.getMetadata()
                                        .getName()
                                        .substring(tr.getMetadata().getName().lastIndexOf("-") + 1),
                                Function.identity()));
    }
}
