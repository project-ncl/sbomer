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
package org.jboss.sbomer.service.nextgen.core.generator;

import java.util.Set;

import org.jboss.sbomer.service.nextgen.core.events.GenerationScheduledEvent;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;

public interface Generator {
    /**
     * A set of supported target deliverable types.
     */
    Set<String> getTypes();

    /**
     * Main entrypoint for requesting the generation. It will receive many events. We need to react only to the ones
     * that we can handle by filtering supported types.
     *
     * @param event
     */
    void onEvent(@Observes(during = TransactionPhase.AFTER_SUCCESS) GenerationScheduledEvent event);
}
