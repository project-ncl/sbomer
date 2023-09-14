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

import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;

import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class ConfigMissingOrGenerated implements Condition<TaskRun, GenerationRequest> {

    @Override
    public boolean isMet(
            DependentResource<TaskRun, GenerationRequest> dependentResource,
            GenerationRequest primary,
            Context<GenerationRequest> context) {

        // Here we are checking whether the configuration exists already or not. In case it's not there, we need to
        // generate one, thus returning true to let the reconciliation happen on the {
        // TaskRunInitDependentResource.
        // In case the config exists and we have some TaskRuns associated with it this means that we generated it.
        // In such case we need to return true as well, to keep the resource. Cleanup of resources are done after
        // successful generation only.
        if (primary.getConfig() == null || !context.getSecondaryResources(TaskRun.class).isEmpty()) {
            return true;
        }

        return false;
    }
}
