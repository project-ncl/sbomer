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
package org.jboss.sbomer.service.feature.sbom.k8s.reconciler.condition;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.sbomer.core.features.sbom.config.runtime.OperationConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationPhase;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;

import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OperationConfigMissingCondition implements Condition<TaskRun, GenerationRequest> {

    Boolean cleanup = ConfigProvider.getConfig()
            .getValue("sbomer.controller.generation-request.cleanup", Boolean.class);

    @Override
    public boolean isMet(
            DependentResource<TaskRun, GenerationRequest> dependentResource,
            GenerationRequest primary,
            Context<GenerationRequest> context) {

        if (!GenerationRequestType.OPERATION.equals(primary.getType())) {
            return false;
        }

        // Here we are checking whether the configuration exists already or not. In case it's not there, we need to
        // generate one, thus returning true to let the reconciliation happen on the
        // TaskRunOperationInitDependentResource.
        // Here, an OperationConfig is needed along with the list of DeliverablesConfig (both may or may not be
        // provided)
        OperationConfig operationConfig = primary.toOperationConfig();
        if (operationConfig == null || operationConfig.getDeliverableUrls() == null
                || operationConfig.getDeliverableUrls().isEmpty()) {
            log.debug("OperationConfigMissingCondition is met: true");
            return true;
        }

        // If the configuration is available (meaning that the initialization phase is finished) and the {@code cleanup}
        // setting is set to false, reconcile. We won't reconcile multiple times at this point, the only thing we want
        // to achieve is that the dependent resource (TaskRun) is retained.
        // We should do this only in the case when there are already some secondary resources.
        if (!cleanup && initTaskRunExist(context)) {
            log.debug("OperationConfigMissingCondition is met: true");
            return true;
        }

        log.debug("OperationConfigMissingCondition is met: false");
        return false;
    }

    private boolean initTaskRunExist(Context<GenerationRequest> context) {
        Set<TaskRun> secondaryResources = context.getSecondaryResources(TaskRun.class);

        // No TaskRuns at all, so no init TaskRun for sure...
        if (secondaryResources.isEmpty()) {
            return false;
        }

        return secondaryResources.stream().anyMatch(taskRun -> {
            Map<String, String> labels = taskRun.getMetadata().getLabels();

            if (labels != null && Objects
                    .equals(labels.get(Labels.LABEL_PHASE), SbomGenerationPhase.OPERATIONINIT.name().toLowerCase())) {
                return true;
            }

            return false;
        });
    }

}
