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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;

/**
 * Labels used by Tekton resources within the SBOM feature.
 */
public class Labels {
    public static final String LABEL_STATUS = "sbomer.jboss.org/status";
    public static final String LABEL_PHASE = "sbomer.jboss.org/phase";
    public static final String LABEL_IDENTIFIER = "sbomer.jboss.org/identifier";
    public static final String LABEL_TYPE = "sbomer.jboss.org/type";
    public static final String LABEL_GENERATION_REQUEST_ID = "sbomer.jboss.org/generation-request-id";
    public static final String LABEL_GENERATION_REQUEST_TYPE = "sbomer.jboss.org/generation-request-type";
    public static final String LABEL_OTEL_TRACE_ID = "sbomer.jboss.org/otel-trace-id";
    public static final String LABEL_OTEL_SPAN_ID = "sbomer.jboss.org/otel-span-id";
    public static final String LABEL_OTEL_TRACEPARENT = "sbomer.jboss.org/otel-traceparent";
  
    private Labels() {
        // This is a utility class
    }

    public static Map<String, String> defaultLabelsToMap(GenerationRequestType sbomGenerationType) {
        Map<String, String> labels = new HashMap<>();

        labels.put("app.kubernetes.io/part-of", "sbomer");
        labels.put("app.kubernetes.io/managed-by", "sbomer");
        labels.put(
                "app.kubernetes.io/instance",
                ConfigProvider.getConfig().getOptionalValue("SBOMER_RELEASE", String.class).orElse("sbomer"));
        labels.put("app.kubernetes.io/component", "generator");
        labels.put(LABEL_TYPE, "generation-request");
        labels.put(LABEL_GENERATION_REQUEST_TYPE, sbomGenerationType.toName());

        return labels;
    }
}
