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
package org.jboss.sbomer.service.feature.sbom.errata.event;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestManifestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventStatus;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class AdvisoryCommentEventListener {

    @Inject
    @RestClient
    ErrataClient errataClient;

    @Inject
    RequestEventRepository repository;

    public void onAdvisoryCommentEvent(@ObservesAsync AdvisoryCommentEvent event) {
        log.debug("Async event received: {}", event.getRequestEvent());

        RequestEvent requestEvent = event.getRequestEvent();
        if (!validateRequestEvent(requestEvent)) {
            return;
        }

        ErrataAdvisoryRequestConfig config = (ErrataAdvisoryRequestConfig) requestEvent.getRequestConfig();
        if (RequestEventStatus.FAILED.equals(requestEvent.getEventStatus())) {
            String comment = "SBOMer failed to generate all manifests!";
            doAddCommentToErratum(comment, config.getAdvisoryId());
            return;
        }

        Errata errata = errataClient.getErratum(config.getAdvisoryId());
        if (!validateErrata(errata, config.getAdvisoryId())) {
            return;
        }

        StringBuilder commentSb = new StringBuilder();
        if (ErrataStatus.QE.equals(errata.getDetails().get().getStatus())) {
            commentSb.append("SBOMer generated build time manifests:").append("\n");
        } else {
            commentSb.append("SBOMer generated release time manifests:").append("\n");
        }

        // Searching all manifest generated
        List<V1Beta1RequestRecord> v1Beta1RequestRecords = repository
                .searchAggregatedResultsNatively("id=" + requestEvent.getId());
        if (v1Beta1RequestRecords == null || v1Beta1RequestRecords.isEmpty()) {
            log.warn(
                    "Could not find any information for the manifests generated from request event {}, ignoring the event!",
                    requestEvent.getId());
            return;
        }

        V1Beta1RequestRecord record = v1Beta1RequestRecords.get(0);
        for (V1Beta1RequestManifestRecord manifest : record.manifests()) {
            commentSb.append("\n").append(manifest.identifier()).append(" [id=").append(manifest.id()).append("]");
        }

        doAddCommentToErratum(commentSb.toString(), config.getAdvisoryId());
    }

    private void doAddCommentToErratum(String comment, String advisoryId) {
        Map<String, String> payload = new HashMap<>();
        payload.put("comment", comment);
        payload.put("type", "AutomatedComment");

        // Serialize to JSON
        try {
            String jsonPayload = ObjectMapperProvider.json().writeValueAsString(payload);
            errataClient.addCommentToErratum(advisoryId, jsonPayload);
        } catch (JsonProcessingException e) {
            log.error("An error occured during the processing of the advisory comment", e);
        } catch (Exception e) {
            log.error("An error occured while adding a comment to errata advisory {}", advisoryId, e);
        }
    }

    private boolean isFinalStatus(RequestEventStatus status) {
        return status == RequestEventStatus.FAILED || status == RequestEventStatus.SUCCESS;
    }

    private boolean isRelevantStatus(ErrataStatus status) {
        return status == ErrataStatus.QE || status == ErrataStatus.SHIPPED_LIVE;
    }

    private boolean validateRequestEvent(RequestEvent requestEvent) {
        if (!isFinalStatus(requestEvent.getEventStatus())) {
            log.warn("The request event is not a final expected status, ignoring the event!");
            return false;
        }
        if (requestEvent.getRequestConfig() == null) {
            log.warn("The event request config is null, ignoring the event!");
            return false;
        }
        if (!(requestEvent.getRequestConfig() instanceof ErrataAdvisoryRequestConfig)) {
            log.warn("The event request config is not of errata advisory type, ignoring the event!");
            return false;
        }
        return true;
    }

    private boolean validateErrata(Errata errata, String advisoryId) {
        if (errata == null || errata.getDetails().isEmpty()) {
            log.warn("Could not find errata advisory details with id '{}', ignoring the event!", advisoryId);
            return false;
        }
        if (!isRelevantStatus(errata.getDetails().get().getStatus())) {
            log.warn("Errata is not in a relevant status (QE | SHIPPED_LIVE), ignoring the event!");
            return false;
        }
        return true;
    }

}
