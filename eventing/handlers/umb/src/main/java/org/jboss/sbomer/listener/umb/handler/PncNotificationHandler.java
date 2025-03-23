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
package org.jboss.sbomer.listener.umb.handler;

import java.util.UUID;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.v1.CloudEventBuilder;
import io.quarkus.vertx.ConsumeEvent;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for PNC build notifications.
 *
 * @author Marek Goldmann
 */
@ApplicationScoped
@Slf4j
public class PncNotificationHandler {

    @RestClient
    BrokerClient brokerClient;

    @ConsumeEvent("umb-pnc")
    public void handle(String message) {
        log.debug("Got an internal event: {}", message);

        CloudEvent cloudEvent = new CloudEventBuilder().withId(UUID.randomUUID().toString())
                .withData("test".getBytes())
                .withType("org.jboss.sbomer.event.umb")
                .build();

        brokerClient.publish(cloudEvent);
    }

}
