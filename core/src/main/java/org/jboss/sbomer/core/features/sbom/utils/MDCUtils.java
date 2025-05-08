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
package org.jboss.sbomer.core.features.sbom.utils;

import java.util.Map;

import org.jboss.pnc.common.Strings;
import org.slf4j.MDC;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MDCUtils {

    public static final String MDC_IDENTIFIER_KEY = "identifier";
    public static final String MDC_TRACE_ID_KEY = "traceId";
    public static final String MDC_SPAN_ID_KEY = "spanId";
    public static final String MDC_TRACEPARENT_KEY = "traceparent";
    public static final String MDC_TRACE_FLAGS_KEY = "traceFlags";
    public static final String MDC_TRACE_STATE_KEY = "traceState";

    private MDCUtils() {
        // This is a utility class
    }

    public static void addIdentifierContext(String identifier) {
        String current = MDC.get(MDC_IDENTIFIER_KEY);
        if (Strings.isEmpty(current)) {
            if (!Strings.isEmpty(identifier)) {
                MDC.put(MDC_IDENTIFIER_KEY, identifier);
            }
        } else {
            log.warn("Did not set new identifierContext [{}] as value already exists [{}].", identifier, current);
        }
    }

    public static void addOtelContext(Map<String, String> otelContextMap) {
        if (otelContextMap == null || otelContextMap.isEmpty()) {
            return;
        }

        for (Map.Entry<?, ?> entry : ((Map<?, ?>) otelContextMap).entrySet()) {
            final Object key = entry.getKey();
            final Object value = entry.getValue();
            if (key != null && value != null) {
                MDC.put(key.toString(), value.toString());
            }
        }
    }

    public static void removeIdentifierContext() {
        MDC.remove(MDC_IDENTIFIER_KEY);
    }

    public static void removeOtelContext() {
        MDC.remove(MDC_TRACE_ID_KEY);
        MDC.remove(MDC_SPAN_ID_KEY);
        MDC.remove(MDC_TRACEPARENT_KEY);
    }

    public static void removeContext() {
        removeIdentifierContext();
        removeOtelContext();
    }

}
