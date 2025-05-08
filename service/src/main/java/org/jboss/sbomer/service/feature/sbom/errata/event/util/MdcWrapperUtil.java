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
package org.jboss.sbomer.service.feature.sbom.errata.event.util;

import io.quarkus.arc.Arc;
import jakarta.enterprise.event.Event;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import org.slf4j.MDC;

public class MdcWrapperUtil {

    private MdcWrapperUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static CompletionStage<MdcEventWrapper> fireAsync(Object payload) {
        Map<String, String> mdcContext = MDC.getCopyOfContextMap();
        Event<Object> event = Arc.container().beanManager().getEvent();

        return event.fireAsync(new MdcEventWrapper(payload, mdcContext));
    }

}
