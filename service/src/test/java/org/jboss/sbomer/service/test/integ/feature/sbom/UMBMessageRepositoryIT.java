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

import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.service.feature.sbom.model.UMBMessage;
import org.jboss.sbomer.service.test.utils.QuarkusTransactionalTest;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

@QuarkusTransactionalTest
@WithKubernetesTestServer
@TestProfile(TestUmbProfile.class)
class UMBMessageRepositoryIT {

    @Test
    void testFindByMsgIdUMBMessage() {
        UMBMessage msg = UMBMessage.find("msgId = ?1", "ID:orch-86-qmrdq-33543-1697588407649-5:1:3:1:1").singleResult();

        assertEquals("ID:orch-86-qmrdq-33543-1697588407649-5:1:3:1:1", msg.getMsgId());
        assertEquals("/topic/VirtualTopic.eng.pnc.builds", msg.getTopic());
        assertEquals(UMBConsumer.PNC, msg.getConsumer());
        assertEquals(UMBMessageStatus.ACK, msg.getStatus());
    }

    @Test
    void testCountMessages() {
        assertEquals(2, UMBMessage.countPncReceivedMessages());
        assertEquals(1, UMBMessage.countPncProcessedMessages());
        assertEquals(1, UMBMessage.countErrataReceivedMessages());
        assertEquals(1, UMBMessage.countErrataProcessedMessages());
    }

}
