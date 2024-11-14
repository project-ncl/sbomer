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
package org.jboss.sbomer.service.feature.sbom.k8s.reconciler;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.tekton.pipeline.v1beta1.TaskRun;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TektonResourceUtils {
    public static enum ResourceTarget {
        CPU, MEMORY
    }

    public static enum ResourceType {
        REQUESTS, LIMITS
    }

    private TektonResourceUtils() {
        // This is a utility class
    }

    public static void adjustComputeResources(TaskRun taskRun) {
        log.debug("Adjusting compute resources for TaskRun '{}'", taskRun.getMetadata().getName());

        ResourceRequirements computeResources = taskRun.getSpec().getComputeResources();

        if (computeResources == null) {
            computeResources = new ResourceRequirements();
        }

        String labelType = taskRun.getMetadata().getLabels().get(Labels.LABEL_TYPE);

        if (labelType == null) {
            log.warn(
                    "Unable to find '{}' label in the '{}' TaskRun, configuring resources will be skipped",
                    Labels.LABEL_TYPE,
                    taskRun.getMetadata().getName());

            return;
        }

        GenerationRequestType requestType = GenerationRequestType.fromName(labelType);

        Map<String, Quantity> requests = new HashMap<>();
        Map<String, Quantity> limits = new HashMap<>();

        try {
            for (ResourceTarget target : ResourceTarget.values()) {
                log.debug("Handling '{}' resource target...", target);

                requests.put(
                        target.toString().toLowerCase(),
                        Quantity.parse(getResources(requestType, ResourceType.REQUESTS, target)));
                limits.put(
                        target.toString().toLowerCase(),
                        Quantity.parse(getResources(requestType, ResourceType.LIMITS, target)));
            }
        } catch (IllegalArgumentException e) {
            log.warn("Unable to set resources for TaskRun '{}'", taskRun.getMetadata().getName(), e);
            return;
        }

        computeResources.setLimits(limits);
        computeResources.setRequests(requests);

        log.info(
                "Updating compute resources of TaskRun '{}' to: {}",
                taskRun.getMetadata().getName(),
                computeResources.toString());

        taskRun.getSpec().setComputeResources(computeResources);
    }

    /**
     * Fetches configured compute resource values for a given generator.
     * 
     * @param generatorType
     * @param type
     * @param target
     * @return
     */
    private static String getResources(GenerationRequestType generatorType, ResourceType type, ResourceTarget target) {
        Optional<String> value = ConfigProvider.getConfig()
                .getOptionalValue(
                        String.format(
                                "sbomer.generator.%s.tekton.resources.%s.%s",
                                generatorType.toName(),
                                type.toString().toLowerCase(),
                                target.toString().toLowerCase()),
                        String.class);

        if (value.isPresent()) {
            return value.get();
        }

        return null;
    }
}
