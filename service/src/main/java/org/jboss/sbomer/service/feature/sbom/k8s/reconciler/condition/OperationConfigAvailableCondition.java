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

import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;

import io.fabric8.tekton.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OperationConfigAvailableCondition implements Condition<TaskRun, GenerationRequest> {

    @Override
    public boolean isMet(
            DependentResource<TaskRun, GenerationRequest> dependentResource,
            GenerationRequest primary,
            Context<GenerationRequest> context) {

        if (!GenerationRequestType.OPERATION.equals(primary.getType())) {
            return false;
        }

        // If the configurations are available, reconcile
        OperationConfig operationConfig = primary.getConfig(OperationConfig.class);

        if (operationConfig != null && operationConfig.getDeliverableUrls() != null
                && !operationConfig.getDeliverableUrls().isEmpty()) {
            log.debug("OperationConfigAvailableCondition is met: true");
            return true;
        }

        log.debug("OperationConfigAvailableCondition is met: false");
        return false;
    }
}
