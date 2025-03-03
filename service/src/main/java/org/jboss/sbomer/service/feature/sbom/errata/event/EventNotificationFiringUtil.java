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
package org.jboss.sbomer.service.feature.sbom.errata.event;

import org.jboss.sbomer.service.feature.sbom.errata.event.comment.RequestEventStatusUpdateEvent;
import org.jboss.sbomer.service.feature.sbom.errata.event.release.StandardAdvisoryReleaseEvent;
import org.jboss.sbomer.service.feature.sbom.errata.event.release.TextOnlyAdvisoryReleaseEvent;

import io.quarkus.arc.Arc;
import jakarta.enterprise.event.Event;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventNotificationFiringUtil {
    private EventNotificationFiringUtil() {
        throw new IllegalStateException("This is a utility class that should not be instantiated");
    }

    public static void notifyRequestEventStatusUpdate(Object requestEventNotification) {
        RequestEventStatusUpdateEvent requestEvent = (RequestEventStatusUpdateEvent) requestEventNotification;
        log.info(
                "Firing async event for status update of request event with id: {} and config: {}",
                requestEvent.getRequestEventId(),
                requestEvent.getRequestEventConfig());
        Event<Object> event = Arc.container().beanManager().getEvent();
        event.fireAsync(requestEventNotification).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Error occurred while processing the async event.", throwable);
            }
        });
    }

    public static void notifyAdvisoryRelease(Object advisoryReleaseNotification) {
        if (advisoryReleaseNotification instanceof StandardAdvisoryReleaseEvent releaseEvent) {
            log.info(
                    "Firing async event for standard advisory release update upon event with id: {}",
                    releaseEvent.getRequestEventId());
        } else {
            TextOnlyAdvisoryReleaseEvent releaseEvent = (TextOnlyAdvisoryReleaseEvent) advisoryReleaseNotification;
            log.info(
                    "Firing async event for text-only advisory release update upon event with id: {}",
                    releaseEvent.getRequestEventId());
        }

        Event<Object> event = Arc.container().beanManager().getEvent();
        event.fireAsync(advisoryReleaseNotification).whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("Error occurred while processing the async event.", throwable);
            }
        });
    }
}
