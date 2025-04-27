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

import org.jboss.pnc.api.constants.MDCHeaderKeys;
import org.jboss.pnc.api.constants.MDCKeys;
import org.jboss.pnc.common.Strings;
import org.slf4j.MDC;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MDCUtils {

    private MDCUtils() {
        // This is a utility class
    }

    public static void addProcessContext(String processContext) {
        String current = MDC.get(MDCKeys.PROCESS_CONTEXT_KEY);
        if (Strings.isEmpty(current)) {
            if (!Strings.isEmpty(processContext)) {
                MDC.put(MDCKeys.PROCESS_CONTEXT_KEY, processContext);
            }
        } else {
            log.warn("Did not set new processContext [{}] as value already exists [{}].", processContext, current);
        }
    }

    public static void addBuildContext(String buildContext) {
        String current = MDC.get(MDCKeys.BUILD_ID_KEY);
        if (Strings.isEmpty(current)) {
            if (!Strings.isEmpty(buildContext)) {
                MDC.put(MDCKeys.BUILD_ID_KEY, buildContext);
            }
        } else {
            log.warn("Did not set new buildContext [{}] as value already exists [{}].", buildContext, current);
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

    public static void removeProcessContext() {
        MDC.remove(MDCKeys.PROCESS_CONTEXT_KEY);
    }

    public static void removeBuildContext() {
        MDC.remove(MDCKeys.BUILD_ID_KEY);
    }

    public static void removeOtelContext() {
        MDC.remove(MDCKeys.TRACE_ID_KEY);
        MDC.remove(MDCKeys.SPAN_ID_KEY);
        MDC.remove(MDCHeaderKeys.TRACEPARENT.getMdcKey());
    }

    public static void removeContext() {
        removeProcessContext();
        removeBuildContext();
        removeOtelContext();
    }

}
