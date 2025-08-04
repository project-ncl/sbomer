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
package org.jboss.sbomer.service.nextgen.core.enums;

// TODO: Ensure statuses are correct
public enum EventStatus {
    /**
     * New event, no work has been performed at all. This is the starting point of the life of an event.
     */
    NEW,
    /**
     * Event ignored. The reason on why it happened should be available in teh reason field of the event.
     */
    IGNORED,
    /**
     * Event is being processed by a resolver.
     */
    RESOLVING,
    /**
     * Resolver finished the work.
     */
    RESOLVED,
    /**
     * Generation initialization is being performed.
     */
    INITIALIZING,
    /**
     * Event is initialized (generations are populated).
     */
    INITIALIZED,
    /**
     * Event is being processed (handled).
     */
    PROCESSING,
    /**
     * Event was successfully processed.
     */
    PROCESSED,
    /**
     * Processing of this event failed. The reason on why it happened should be available in teh reason field of the
     * event.
     */
    ERROR;

    public static EventStatus fromName(String origin) {
        return EventStatus.valueOf(origin.toUpperCase());
    }

    public String toName() {
        return this.name().toUpperCase();
    }
}
