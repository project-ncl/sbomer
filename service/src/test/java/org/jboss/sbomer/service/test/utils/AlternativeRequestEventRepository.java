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
package org.jboss.sbomer.service.test.utils;

import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.RequestEventType;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;

import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Singleton;

@Alternative
@Singleton
public class AlternativeRequestEventRepository extends RequestEventRepository {

    @Override
    public long countAckedUMBEventsFrom(UMBConsumer consumer) {
        String q = "SELECT * FROM request WHERE event_type = :event_type "
                + " AND JSON_EXTRACT(event, '$.consumer') = :consumer "
                + " AND JSON_EXTRACT(event, '$.msg_type') <> :msg_type "
                + " AND JSON_EXTRACT(event, '$.msg_status') = :msg_status";

        return getEntityManager().createNativeQuery(q, RequestEvent.class)
                .setParameter(RequestEvent.REQUEST_EVENT_TYPE, RequestEventType.UMB.name())
                .setParameter(RequestEvent.EVENT_KEY_UMB_MSG_TYPE, RequestEvent.EVENT_VALUE_UMB_UNKNOWN_MSG_TYPE)
                .setParameter(RequestEvent.EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.ACK.name())
                .setParameter(RequestEvent.EVENT_KEY_UMB_CONSUMER, consumer.name())
                .getResultList()
                .size();
    }

    @Override
    public long countAllUMBEventsFrom(UMBConsumer consumer) {
        String q = "SELECT * FROM request WHERE event_type = :event_type "
                + " AND JSON_EXTRACT(event, '$.consumer') = :consumer";

        return getEntityManager().createNativeQuery(q, RequestEvent.class)
                .setParameter(RequestEvent.REQUEST_EVENT_TYPE, RequestEventType.UMB.name())
                .setParameter(RequestEvent.EVENT_KEY_UMB_CONSUMER, consumer.name())
                .getResultList()
                .size();
    }

    @Override
    public long countEventsForTypeAndIdentifier(String typeValue, String identifierKey, String identifierValue) {

        String q = "SELECT * FROM request WHERE JSON_EXTRACT(event, '$.type') = :type " + " AND JSON_EXTRACT(event, '$."
                + identifierKey + "') = :identifierValue";

        return getEntityManager().createNativeQuery(q, RequestEvent.class)
                .setParameter("type", typeValue)
                .setParameter("identifierValue", identifierValue)
                .getResultList()
                .size();
    }
}