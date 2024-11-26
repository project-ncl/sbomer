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
package org.jboss.sbomer.service.test.integ.feature.sbom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.config.request.PncBuildRequestConfig;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventType;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;
import org.jboss.sbomer.service.test.utils.QuarkusTransactionalTest;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@QuarkusTransactionalTest
@WithKubernetesTestServer
@TestProfile(TestUmbProfile.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class RequestEventRepositoryIT {

    @Inject
    RequestEventRepository repository;

    @Test
    @Order(1)
    void testFindById() {
        RequestEvent requestEvent = RequestEvent.findById("errata_139787");
        assertEquals(RequestEventType.UMB, requestEvent.getEventType());

        RequestConfig config = requestEvent.getRequestConfig();
        assertInstanceOf(ErrataAdvisoryRequestConfig.class, config);
        assertEquals(ErrataAdvisoryRequestConfig.TYPE_NAME, config.getType());

        ErrataAdvisoryRequestConfig advisoryConfig = (ErrataAdvisoryRequestConfig) config;
        assertEquals("139787", advisoryConfig.getAdvisoryId());

        JsonNode event = requestEvent.getEvent();
        assertEquals(
                "topic://VirtualTopic.eng.errata.activity.status",
                event.get(RequestEvent.EVENT_KEY_UMB_TOPIC).asText());
        assertEquals(UMBConsumer.ERRATA.name(), event.get(RequestEvent.EVENT_KEY_UMB_CONSUMER).asText());
        assertEquals(UMBMessageStatus.ACK.name(), event.get(RequestEvent.EVENT_KEY_UMB_MSG_STATUS).asText());
        assertEquals(ErrataAdvisoryRequestConfig.TYPE_NAME, event.get(RequestEvent.EVENT_KEY_UMB_MSG_TYPE).asText());
    }

    @Test
    @Order(2)
    void testCountMessages() {
        assertEquals(3, repository.countPncReceivedMessages());
        assertEquals(1, repository.countErrataReceivedMessages());
        assertEquals(2, repository.countPncProcessedMessages());
        assertEquals(1, repository.countErrataProcessedMessages());
        assertEquals(1, repository.countAllEventsOfType(RequestEventType.REST));
        assertEquals(
                2,
                repository.countEventsForConfigWithIdentifierValue(PncBuildRequestConfig.class, "ARYT3LBXDVYAC"));
    }

}
