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
package org.jboss.sbomer.service.nextgen.controller.tekton;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;
import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;

import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.tekton.v1beta1.TaskRunStepOverride;
import io.fabric8.tekton.v1beta1.TaskRunStepOverrideBuilder;

public class TektonUtilities {

    private TektonUtilities() {

    }

    public static TaskRunStepOverride resourceOverrides(GenerationRequest request) {

        return new TaskRunStepOverrideBuilder().withName("generate")
                .withNewResources()
                .withRequests(
                        Map.of(
                                "cpu",
                                new Quantity(request.generator().config().resources().requests().cpu()),
                                "memory",
                                new Quantity(request.generator().config().resources().requests().memory())))
                .withLimits(
                        Map.of(
                                "cpu",
                                new Quantity(request.generator().config().resources().limits().cpu()),
                                "memory",
                                new Quantity(request.generator().config().resources().limits().memory())))
                .endResources()
                .build();

    }

    public static Map<String, String> createBasicGenerationLabels(GenerationRecord generation, String generatorName) {

        Map<String, String> labels = new HashMap<>();

        labels.put(AbstractTektonController.GENERATION_ID_LABEL, generation.id());
        labels.put(AbstractTektonController.GENERATOR_TYPE, generatorName);

        Optional.ofNullable(generation.metadata())
                .map(meta -> meta.get("otelTraceId"))
                .ifPresent(traceId -> labels.put(Labels.LABEL_OTEL_TRACE_ID, traceId));

        Optional.ofNullable(generation.metadata())
                .map(meta -> meta.get("otelSpanId"))
                .ifPresent(spanId -> labels.put(Labels.LABEL_OTEL_SPAN_ID, spanId));

        Optional.ofNullable(generation.metadata())
                .map(meta -> meta.get("otelTraceParent"))
                .ifPresent(traceParent -> labels.put(Labels.LABEL_OTEL_TRACEPARENT, traceParent));

        return labels;

    }

    public static TaskRunStepOverride multiplyMemoryOverrides(
            TaskRunStepOverride originalStepOverride,
            double multiplier) {
        ResourceRequirements resources = originalStepOverride.getResources();
        Quantity cpuRequestsQuantity = resources.getRequests().get(AbstractTektonController.CPU_OVERRIDE);
        Quantity memoryRequestsQuantity = resources.getRequests().get(AbstractTektonController.MEMORY_OVERRIDE);
        Quantity cpuLimitsQuantity = resources.getLimits().get(AbstractTektonController.CPU_OVERRIDE);
        Quantity memoryLimitsQuantity = resources.getLimits().get(AbstractTektonController.MEMORY_OVERRIDE);

        return new TaskRunStepOverrideBuilder().withName(AbstractTektonController.GENERATE_OVERRIDE)
                .withNewResources()
                .withRequests(
                        Map.of(
                                AbstractTektonController.CPU_OVERRIDE,
                                new Quantity(cpuRequestsQuantity.getAmount(), cpuRequestsQuantity.getFormat()),
                                AbstractTektonController.MEMORY_OVERRIDE,
                                multiplyMemory(memoryRequestsQuantity, multiplier)))
                .withLimits(
                        Map.of(
                                AbstractTektonController.CPU_OVERRIDE,
                                new Quantity(cpuLimitsQuantity.getAmount(), cpuLimitsQuantity.getFormat()),
                                AbstractTektonController.MEMORY_OVERRIDE,
                                multiplyMemory(memoryLimitsQuantity, multiplier)))
                .endResources()
                .build();
    }

    private static Quantity multiplyMemory(Quantity originalQuantity, double multiplier) {
        int value = Integer.parseInt(originalQuantity.getAmount());
        return new Quantity((int) Math.ceil(value * multiplier) + originalQuantity.getFormat());
    }

}
