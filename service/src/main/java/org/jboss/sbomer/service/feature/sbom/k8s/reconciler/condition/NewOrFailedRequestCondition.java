/**
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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.sbomer.core.features.sbomer.utils.MDCUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;

import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NewOrFailedRequestCondition implements Condition<TaskRun, GenerationRequest> {

    @Override
    public boolean isMet(GenerationRequest primary, TaskRun secondary, Context<GenerationRequest> context) {
        MDCUtils.addBuildContext(primary.getBuildId());

        if (primary.getStatus() != null && !primary.getStatus().isFinal()) {
            return true;
        }

        AtomicBoolean shouldProceed = new AtomicBoolean(false);

        context.getSecondaryResources(TaskRun.class).forEach(tr -> {
            if (isFailed(tr)) {
                shouldProceed.setPlain(true);
            }
        });

        if (shouldProceed.get()) {
            return true;
        }

        return true;
    }

    /**
     * Returns {@code true} in case the {@link TaskRun} is in a failed but finished state.
     *
     * @param taskRun
     * @return
     */
    private boolean isFailed(TaskRun taskRun) {
        log.debug("Checking whether the TaskRun '{}' is in failed state", taskRun.getMetadata().getName());

        if (taskRun.getStatus() != null && taskRun.getStatus().getConditions() != null
                && taskRun.getStatus().getConditions().size() > 0
                && Objects.equals(taskRun.getStatus().getConditions().get(0).getStatus(), "False")) {

            log.debug("TaskRun '{}' is in failed state", taskRun.getMetadata().getName());
            return true;
        }

        log.debug("TaskRun '{}' is not in failed state", taskRun.getMetadata().getName());

        return false;

    }

}
