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
package org.jboss.sbomer.service.nextgen.generator.koji;

import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.FileUtils;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.leader.LeaderManager;
import org.jboss.sbomer.service.nextgen.controller.tekton.AbstractTektonController;
import org.jboss.sbomer.service.nextgen.controller.tekton.TektonUtilities;
import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationResult;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.rest.SBOMerClient;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.service.EntityMapper;

import com.fasterxml.jackson.databind.JsonNode;

import io.fabric8.kubernetes.api.model.Duration;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.tekton.v1beta1.Param;
import io.fabric8.tekton.v1beta1.ParamBuilder;
import io.fabric8.tekton.v1beta1.TaskRefBuilder;
import io.fabric8.tekton.v1beta1.TaskRun;
import io.fabric8.tekton.v1beta1.TaskRunBuilder;
import io.fabric8.tekton.v1beta1.TaskRunStepOverride;
import io.fabric8.tekton.v1beta1.TaskRunStepOverrideBuilder;
import io.fabric8.tekton.v1beta1.WorkspaceBindingBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class KojiGenerator extends AbstractTektonController {

    public static final String PARAM_BREW_BUILD_ID = "build-id";

    public static final String TASK_SUFFIX = "-generator-brew-rpm";
    public static final String SA_SUFFIX = "-sa";

    public static final String GENERATOR_NAME = "koji";
    public static final String GENERATOR_VERSION = "0.1.0";

    private KojiGenerator() {
        super(null, null, null, null, null, null);
    }

    @Inject
    public KojiGenerator(
            @RestClient SBOMerClient sbomerClient,
            KubernetesClient kubernetesClient,
            GenerationRequestControllerConfig controllerConfig,
            ManagedExecutor managedExecutor,
            EntityMapper mapper,
            LeaderManager leaderManager) {
        super(sbomerClient, kubernetesClient, controllerConfig, managedExecutor, mapper, leaderManager);
    }

    @Override
    public Set<String> getSupportedTypes() {
        return Set.of("BREW_RPM");
    }

    @Override
    public String getGeneratorName() {
        return GENERATOR_NAME;
    }

    @Override
    public String getGeneratorVersion() {
        return GENERATOR_VERSION;
    }

    @Override
    public void generate(GenerationRecord generationRecord) {
        log.info("Preparing Tekton Task Run for generation '{}'", generationRecord.id());

        TaskRun desired = desired(generationRecord);

        log.trace("Prepared TaskRun for generation '{}': {}", generationRecord.id(), desired);

        try {
            kubernetesClient.resources(TaskRun.class).resource(desired).create();
        } catch (KubernetesClientException e) {
            log.warn("Unable to schedule Tekton TaskRun", e);

            updateStatus(
                    generationRecord.id(),
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_SYSTEM,
                    "Unable to schedule Tekton TaskRun: {}",
                    e.getMessage());

            return;
        }
    }

    @Override
    public TaskRun desired(GenerationRecord generation) {

        MDCUtils.removeOtelContext();

        log.debug(
                "Preparing dependent resource for the '{}' phase related to Generation with id '{}'",
                SbomGenerationPhase.GENERATE,
                generation.id());

        // TODO: populate traces when we create generations
        Map<String, String> labels = TektonUtilities.createBasicGenerationLabels(generation, getGeneratorName());

        GenerationRequest request = JacksonUtils.parse(GenerationRequest.class, generation.request());

        Duration timeout;

        try {
            timeout = Duration.parse("6h");
        } catch (ParseException e) {
            throw new ApplicationException("Cannot set timeout", e);
        }

        String taskSuffix = null;
        List<Param> params = new ArrayList<>();

        // Select Tekton Task and set parameters depending on the type of the target
        switch (request.target().type()) {
            case "BREW_RPM":
                taskSuffix = TASK_SUFFIX;

                params.add(
                        new ParamBuilder().withName(PARAM_BREW_BUILD_ID)
                                .withNewValue(request.target().identifier())
                                .build());
                break;

            default:
                break;
        }

        if (taskSuffix == null) {
            throw new ApplicationException("Unknown target type: {}", request.target().type());
        }

        TaskRun taskRun = new TaskRunBuilder().withNewMetadata()
                .withLabels(labels)
                .withName(
                        "generation-" + generation.id().toLowerCase() + "-" + SbomGenerationPhase.GENERATE.ordinal()
                                + "-" + SbomGenerationPhase.GENERATE.name().toLowerCase())
                .endMetadata()
                .withNewSpec()
                .withServiceAccountName(release + SA_SUFFIX)
                .withTimeout(timeout)
                .withParams(params)
                .withTaskRef(new TaskRefBuilder().withName(release + TASK_SUFFIX).build())
                .withStepOverrides(TektonUtilities.resourceOverrides(request))
                .withWorkspaces(
                        new WorkspaceBindingBuilder().withSubPath(generation.id())
                                .withName("data")
                                .withPersistentVolumeClaim(
                                        new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(release + "-sboms")
                                                .build())
                                .build())
                .endSpec()
                .build();
        Deployment deployment = kubernetesClient.apps().deployments().withName(release + "-service").get();

        if (deployment != null) {
            log.debug("Setting SBOMer deployment as the owner for the newly created TaskRun");

            taskRun.getMetadata()
                    .setOwnerReferences(
                            Collections.singletonList(
                                    new OwnerReferenceBuilder().withKind(HasMetadata.getKind(Deployment.class))
                                            .withApiVersion(HasMetadata.getApiVersion(Deployment.class))
                                            .withName(release + "-service")
                                            .withUid(deployment.getMetadata().getUid())
                                            .build()));
        }
        return taskRun;
    }

}