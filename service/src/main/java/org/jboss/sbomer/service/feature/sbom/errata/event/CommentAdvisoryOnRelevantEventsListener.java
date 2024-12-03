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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestManifestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventStatus;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class CommentAdvisoryOnRelevantEventsListener {

    @ConfigProperty(name = "SBOMER_ROUTE_HOST", defaultValue = "sbomer")
    String sbomerHost;

    @Inject
    @RestClient
    ErrataClient errataClient;

    @Inject
    RequestEventRepository requestEventRepository;

    @Inject
    SbomRepository sbomRepository;

    @Inject
    FeatureFlags featureFlags;

    public void onRequestEventStatusUpdate(@ObservesAsync RequestEventStatusUpdateEvent event) {
        log.debug("Advisory comment event received for request event completion: {}", event.getRequestEvent());
        if (!featureFlags.errataCommentsGenerationsEnabled()) {
            log.warn(
                    "Errata comments generation feature is disabled, no comments will be added to the Errata advisory!!");
            return;
        }

        RequestEvent requestEvent = event.getRequestEvent();
        if (!isValidRequestEvent(requestEvent)) {
            return;
        }

        ErrataAdvisoryRequestConfig config = (ErrataAdvisoryRequestConfig) requestEvent.getRequestConfig();
        Errata errata = errataClient.getErratum(config.getAdvisoryId());
        if (!isValidErrata(errata, config.getAdvisoryId())) {
            return;
        }

        // Searching all manifest generated from this request event
        List<V1Beta1RequestRecord> v1Beta1RequestRecords = requestEventRepository
                .searchAggregatedResultsNatively("id=" + requestEvent.getId());
        if (v1Beta1RequestRecords == null || v1Beta1RequestRecords.isEmpty()) {
            log.warn(
                    "Could not find any information for the manifests generated from request event {}, ignoring the event!",
                    requestEvent.getId());
            return;
        }
        V1Beta1RequestRecord requestRecord = v1Beta1RequestRecords.get(0);

        StringBuilder commentSb = new StringBuilder();
        List<String> processedGenerationsIds = new ArrayList<String>();

        String summaryStatusSection = createSummaryStatusSection(requestRecord);
        String succeededGenerationSection = createGenerationsSectionForStatus(
                requestRecord,
                processedGenerationsIds,
                SbomGenerationStatus.FINISHED,
                "\nSucceeded generations:\n");
        String failedGenerationSection = createGenerationsSectionForStatus(
                requestRecord,
                processedGenerationsIds,
                SbomGenerationStatus.FAILED,
                "\nFailed generations:\n");
        String finalSection = generateRequestEventFinalSection(requestEvent);
        commentSb.append(summaryStatusSection)
                .append(succeededGenerationSection)
                .append(failedGenerationSection)
                .append(finalSection);

        doAddCommentToErratum(commentSb.toString(), config.getAdvisoryId());
    }

    @Retry(maxRetries = 10)
    public void doAddCommentToErratum(String comment, String advisoryId) {
        Map<String, String> payload = new HashMap<>();
        payload.put("comment", comment);
        payload.put("type", "AutomatedComment");

        // Serialize to JSON
        try {
            String jsonPayload = ObjectMapperProvider.json().writeValueAsString(payload);
            errataClient.addCommentToErratum(advisoryId, jsonPayload);
        } catch (JsonProcessingException e) {
            log.error("An error occured during the processing of the advisory comment", e);
        }
    }

    private String createSummaryStatusSection(V1Beta1RequestRecord requestRecord) {
        Map<SbomGenerationStatus, Long> generationsCounts = countGenerationsByStatus(requestRecord);
        long successful = generationsCounts.getOrDefault(SbomGenerationStatus.FINISHED, 0L);
        long failed = generationsCounts.getOrDefault(SbomGenerationStatus.FAILED, 0L);

        return (successful + failed) + " builds manifested. " + successful + " generations succeeded," + failed
                + " failed.\n\n";
    }

    private String createGenerationsSectionForStatus(
            V1Beta1RequestRecord requestRecord,
            List<String> processedGenerationsIds,
            SbomGenerationStatus status,
            String prefix) {

        StringBuilder generationsSection = new StringBuilder(prefix);
        if (requestRecord.manifests() != null && !requestRecord.manifests().isEmpty()) {
            for (V1Beta1RequestManifestRecord manifest : requestRecord.manifests()) {
                if (processedGenerationsIds.contains(manifest.generation().id())) {
                    continue;
                }

                SbomGenerationStatus generationStatus = SbomGenerationStatus.fromName(manifest.generation().status());
                if (status.equals(generationStatus)) {

                    String nvr = getGenerationNVRFromManifest(manifest);
                    generationsSection.append("\n")
                            .append(nvr)
                            .append("https://" + sbomerHost + "/generations/" + manifest.generation().id());
                    processedGenerationsIds.add(manifest.generation().id());
                }
            }
        }
        return generationsSection.toString();
    }

    private String generateRequestEventFinalSection(RequestEvent requestEvent) {
        return "\n\nList of all manifests generated: " + "https://" + sbomerHost + "/requestevents/"
                + requestEvent.getId() + "\n";
    }

    private String getGenerationNVRFromManifest(V1Beta1RequestManifestRecord manifestRecord) {
        GenerationRequestType generationRequestType = GenerationRequestType
                .fromName(manifestRecord.generation().type());

        if (GenerationRequestType.BREW_RPM.equals(generationRequestType)) {
            // The NVR is stored as the generation identifier
            return manifestRecord.generation().identifier() + ": ";
        }

        if (GenerationRequestType.CONTAINERIMAGE.equals(generationRequestType)) {
            // The NVR is not stored inside the generation, we need to get it from the manifest. Might be optimized in
            // future.
            Sbom sbom = sbomRepository.findById(manifestRecord.id());
            String nvr = SbomUtils.computeNVRFromContainerManifest(sbom.getSbom());

            return nvr != null ? (nvr + ": ") : "";
        }

        // The are no NVRs associated with GenerationRequestType.BUILD or GenerationRequestType.OPERATION or
        // GenerationRequestType.ANALYSIS
        return "";
    }

    private boolean isValidRequestEvent(RequestEvent requestEvent) {
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

    private boolean isValidErrata(Errata errata, String advisoryId) {
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

    private boolean isFinalStatus(RequestEventStatus status) {
        return status == RequestEventStatus.FAILED || status == RequestEventStatus.SUCCESS;
    }

    private boolean isRelevantStatus(ErrataStatus status) {
        return status == ErrataStatus.QE || status == ErrataStatus.SHIPPED_LIVE;
    }

    private Map<SbomGenerationStatus, Long> countGenerationsByStatus(V1Beta1RequestRecord requestRecord) {
        return Optional.ofNullable(requestRecord.manifests())
                .orElse(Collections.emptyList())
                .stream()
                .map(V1Beta1RequestManifestRecord::generation)
                .filter(Objects::nonNull)
                .distinct()
                .collect(
                        Collectors.groupingBy(
                                generation -> SbomGenerationStatus.fromName(generation.status()),
                                Collectors.counting()));
    }

}
