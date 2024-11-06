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
package org.jboss.sbomer.service.feature.sbom.features.umb.consumer;

import static org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus.QE;
import static org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus.SHIPPED_LIVE;

import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG;

import java.io.IOException;

import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataMessageHelper;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.ErrataStatusChangeMessageBody;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.service.AdvisoryService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ErrataNotificationHandler {

    @Inject
    @Setter
    AdvisoryService advisoryService;

    @Inject
    @Setter
    FeatureFlags featureFlags;

    public void handle(RequestEvent requestEvent) throws JsonProcessingException, IOException {

        if (!featureFlags.errataIntegrationEnabled()) {
            log.warn("Errata API integration is disabled, the UMB message won't be used!!");
            return;
        }

        JsonNode msgNode = requestEvent.getEvent().get(EVENT_KEY_UMB_MSG);
        String msg = msgNode.isTextual() ? msgNode.textValue() : msgNode.toString();
        ErrataStatusChangeMessageBody errataStatusChange = ErrataMessageHelper.fromStatusChangeMessage(msg);

        if (!isRelevantStatus(errataStatusChange.getStatus())) {
            log.warn("Received a status change that is not QE nor SHIPPED_LIVE, ignoring it");
            return;
        }

        // Update the requestEvent with the requestConfig
        requestEvent = addErrataAdvisoryRequestConfig(requestEvent, String.valueOf(errataStatusChange.getErrataId()));

        advisoryService.generateFromAdvisory(requestEvent);
    }

    private boolean isRelevantStatus(ErrataStatus status) {
        return status == QE || status == SHIPPED_LIVE;
    }

    @Transactional
    protected RequestEvent addErrataAdvisoryRequestConfig(RequestEvent requestEvent, String errataId) {
        requestEvent.setRequestConfig(ErrataAdvisoryRequestConfig.builder().withAdvisoryId(errataId).build());
        return requestEvent.save();
    }

}
