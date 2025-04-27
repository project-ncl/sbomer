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

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;

/**
 * Labels used by Tekton resources within the SBOM feature.
 */
public class Labels {
    public static final String LABEL_TYPE = "sbomer.jboss.org/generation-request-type";
    public static final String LABEL_STATUS = "sbomer.jboss.org/status";
    public static final String LABEL_PHASE = "sbomer.jboss.org/phase";
    public static final String LABEL_IDENTIFIER = "sbomer.jboss.org/identifier";
    public static final String LABEL_GENERATION_REQUEST_ID = "sbomer.jboss.org/generation-request-id";
    public static final String LABEL_OTEL_TRACE_ID = "sbomer.jboss.org/otel-trace-id";
    public static final String LABEL_OTEL_SPAN_ID = "sbomer.jboss.org/otel-span-id";
    public static final String LABEL_OTEL_TRACEPARENT = "sbomer.jboss.org/otel-traceparent";

    // The selector used but the only (atm) reconciler is generic, without the generation request type label, so to
    // select all types
    public static final String LABEL_SELECTOR = "app.kubernetes.io/part-of=sbomer,app.kubernetes.io/component=sbom,app.kubernetes.io/managed-by=sbom,sbomer.jboss.org/type=generation-request";

    private Labels() {
        // This is a utility class
    }

    public static Map<String, String> defaultLabelsToMap(GenerationRequestType sbomGenerationType) {

        Map<String, String> labels = Arrays.stream(LABEL_SELECTOR.split(","))
                .map(l -> l.split("="))
                .collect(Collectors.toMap(splitLabel -> splitLabel[0], splitLabel -> splitLabel[1]));

        labels.put(LABEL_TYPE, sbomGenerationType.toName());
        return labels;
    }
}
