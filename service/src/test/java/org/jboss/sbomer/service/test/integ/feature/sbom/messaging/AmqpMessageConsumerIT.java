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
package org.jboss.sbomer.service.test.integ.feature.sbom.messaging;

import java.io.IOException;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.AmqpMessageConsumer;
import org.jboss.sbomer.service.test.utils.AmqpMessageHelper;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;

@QuarkusTest
@WithTestResource(AmqpTestResourceLifecycleManager.class)
class AmqpMessageConsumerIT {
    @Inject
    AmqpMessageConsumer consumer;

    @Test
    void testParsingOfUnexpectedData() throws IOException {
        Message<String> message = AmqpMessageHelper.toMessage(
                TestResources.asString("umb/unexpected.json"),
                new JsonObject().put("type", "DeliverableAnalysisStateChange"));

        consumer.process(message);
    }
}
