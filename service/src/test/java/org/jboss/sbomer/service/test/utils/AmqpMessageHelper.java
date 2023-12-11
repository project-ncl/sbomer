/**
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
package org.jboss.sbomer.service.test.utils;

import org.eclipse.microprofile.reactive.messaging.Message;

import io.smallrye.reactive.messaging.amqp.IncomingAmqpMetadata;
import io.smallrye.reactive.messaging.amqp.OutgoingAmqpMetadata;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.vertx.amqp.AmqpMessageBuilder;
import io.vertx.core.json.JsonObject;

public class AmqpMessageHelper {

    /**
     * It looks that {@link InMemoryConnector} is not able to convert {@link OutgoingAmqpMetadata} from an outgoing
     * message into {@link IncomingAmqpMetadata} for an incoming message, therefore we need to send messages with
     * {@link IncomingAmqpMetadata}...
     *
     * @param payload The body to send
     * @param headers Headers to attach to the message
     * @see https://github.com/smallrye/smallrye-reactive-messaging/issues/2407
     * @return {@link Message} containing {@code payload} and {@code headers}
     */
    public static Message<String> toMessage(String payload, JsonObject headers) {
        return Message.of(payload)
                .addMetadata(
                        new IncomingAmqpMetadata(AmqpMessageBuilder.create().applicationProperties(headers).build()));
    }
}
