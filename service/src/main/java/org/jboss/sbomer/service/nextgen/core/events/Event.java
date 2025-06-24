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
package org.jboss.sbomer.service.nextgen.core.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        visible = true,
        property = "type")
@JsonSubTypes({ @Type(EventStatusChangeEvent.class), @Type(GenerationStatusChangeEvent.class),
        @Type(ResolveRequestEvent.class) })
public interface Event {
    /**
     * The type (name) of the event.
     * 
     * @return Type of the event
     */
    String type();

    /**
     * Key that is used for scheduling and storage purposes.
     * 
     * @see https://www.confluent.io/learn/kafka-message-key/#what-is-a-kafka-message-key
     * @return A string being the key
     */
    String key();
}
