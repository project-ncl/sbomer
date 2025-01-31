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
package org.jboss.sbomer.service.feature.sbom.errata.event.comment;

import java.util.ArrayList;
import java.util.Collections;
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
import org.jboss.sbomer.service.feature.sbom.errata.event.AdvisoryEventUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

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
    SbomService sbomService;

    @Inject
    FeatureFlags featureFlags;

    public void onRequestEventStatusUpdate(@ObservesAsync RequestEventStatusUpdateEvent event) {
        log.debug("Event received for request event status update...");
        if (!featureFlags.errataCommentsGenerationsEnabled()) {
            log.warn(
                    "Errata comments generation feature is disabled, no comments will be added to the Errata advisory!!");
            return;
        }

        if (!isValidRequestEvent(event)) {
            return;
        }

        ErrataAdvisoryRequestConfig config = (ErrataAdvisoryRequestConfig) event.getRequestEventConfig();
        Errata errata = errataClient.getErratum(config.getAdvisoryId());
        if (!isValidErrata(errata, config.getAdvisoryId())) {
            return;
        }

        if (event.getRequestEventId() != null) {
            // Advisories which produced request events which handled the manifestation.
            // Standard advisories and text-only advisories with "deliverables" notes content fall in this category.
            handleAutomatedManifestationAdvisory(event, config);
        } else {
            // Advisories whose manifestation was handled autonomously.
            // Text-only advisories with "manifest" notes content fall in this category.
            handleManualManifestationAdvisory(errata, config);
        }
    }

    private void handleAutomatedManifestationAdvisory(
            RequestEventStatusUpdateEvent event,
            ErrataAdvisoryRequestConfig config) {
        List<V1Beta1RequestRecord> requestRecords = sbomService
                .searchAggregatedResultsNatively("id=" + event.getRequestEventId());

        if (requestRecords == null || requestRecords.isEmpty()) {
            log.warn(
                    "Could not find information for manifests generated from request event {}, ignoring the event!",
                    event.getRequestEventId());
            return;
        }

        V1Beta1RequestRecord requestRecord = requestRecords.get(0);
        List<String> processedGenerationsIds = new ArrayList<>();
        Map<SbomGenerationStatus, Long> generationsCounts = countGenerationsByStatus(requestRecord);

        StringBuilder commentSb = new StringBuilder();
        commentSb.append(createSummaryStatusSection(generationsCounts))
                .append(
                        createGenerationsSectionForStatus(
                                requestRecord,
                                processedGenerationsIds,
                                SbomGenerationStatus.FINISHED,
                                "\nSucceeded generations:\n"))
                .append(
                        createGenerationsSectionForStatus(
                                requestRecord,
                                processedGenerationsIds,
                                SbomGenerationStatus.FAILED,
                                "\nFailed generations:\n"))
                .append(generateRequestEventFinalSection(event.getRequestEventId()));

        log.debug("Adding comment to advisoryId {} : '{}'", config.getAdvisoryId(), commentSb);
        doAddCommentToErratum(commentSb.toString(), config.getAdvisoryId());
    }

    private void handleManualManifestationAdvisory(Errata errata, ErrataAdvisoryRequestConfig config) {
        JsonNode notesMapping = errata.getNotesMapping().orElse(null);
        if (notesMapping == null || !notesMapping.has("manifest")) {
            log.warn(
                    "The text-only advisory {} does not have 'manifests' listed, no comments will be added!",
                    config.getAdvisoryId());
            return;
        }

        List<String> manifestsPurls = AdvisoryEventUtils.extractPurlUrisFromManifestNode(notesMapping);
        List<SbomGenerationRequest> generations = findGenerationsByPurls(manifestsPurls);

        List<String> processedGenerationsIds = new ArrayList<>();
        Map<SbomGenerationStatus, Long> generationsCounts = countGenerationsByStatus(generations);

        StringBuilder commentSb = new StringBuilder();
        commentSb.append(createSummaryStatusSection(generationsCounts))
                .append(
                        createGenerationsSectionForStatus(
                                generations,
                                processedGenerationsIds,
                                SbomGenerationStatus.FINISHED,
                                "\nSucceeded generations:\n"))
                .append(
                        createGenerationsSectionForStatus(
                                generations,
                                processedGenerationsIds,
                                SbomGenerationStatus.FAILED,
                                "\nFailed generations:\n"));

        log.debug("Adding comment to advisoryId {} : '{}'", config.getAdvisoryId(), commentSb);
        doAddCommentToErratum(commentSb.toString(), config.getAdvisoryId());
    }

    @Retry(maxRetries = 10)
    public void doAddCommentToErratum(String comment, String advisoryId) {
        try {
            String jsonPayload = ObjectMapperProvider.json()
                    .writeValueAsString(Map.of("comment", comment, "type", "AutomatedComment"));
            errataClient.addCommentToErratum(advisoryId, jsonPayload);
        } catch (JsonProcessingException e) {
            log.error("An error occurred during the processing of the advisory comment", e);
        }
    }

    private List<SbomGenerationRequest> findGenerationsByPurls(List<String> purls) {
        List<SbomGenerationRequest> generations = new ArrayList<>();
        purls.forEach(purl -> {
            Sbom sbom = sbomService.findByPurl(purl);
            if (sbom != null && sbom.getGenerationRequest() != null) {
                generations.add(sbom.getGenerationRequest());
            }
        });
        return generations;
    }

    private String createSummaryStatusSection(Map<SbomGenerationStatus, Long> generationsCounts) {
        if (generationsCounts.isEmpty()) {
            // If there are no manifests generated
            return "0 builds manifested. 0 generations succeeded, all failed.\n\n";
        }

        long successful = generationsCounts.getOrDefault(SbomGenerationStatus.FINISHED, 0L);
        long failed = generationsCounts.getOrDefault(SbomGenerationStatus.FAILED, 0L);
        return String.format(
                "%d builds manifested. %d generations succeeded, %d failed.%n%n",
                (successful + failed),
                successful,
                failed);
    }

    private String createGenerationsSectionForStatus(
            V1Beta1RequestRecord requestRecord,
            List<String> processedGenerationsIds,
            SbomGenerationStatus status,
            String prefix) {

        StringBuilder generationsSection = new StringBuilder();
        if (requestRecord.manifests() != null && !requestRecord.manifests().isEmpty()) {
            for (V1Beta1RequestManifestRecord manifest : requestRecord.manifests()) {
                if (processedGenerationsIds.contains(manifest.generation().id())) {
                    continue;
                }

                SbomGenerationStatus generationStatus = SbomGenerationStatus.fromName(manifest.generation().status());
                if (status.equals(generationStatus)) {

                    String nvr = getGenerationNVRFromManifest(manifest);
                    generationsSection.append("\n")
                        .append(nvr).append("https://").append(sbomerHost).append("/generations/").append(manifest.generation().id());
                    processedGenerationsIds.add(manifest.generation().id());
                }
            }
        }
        // If no generations were added, do not output anything in the comment
        if (!generationsSection.isEmpty()) {
            generationsSection.insert(0, prefix);
        }

        return generationsSection.toString();
    }

    private String createGenerationsSectionForStatus(
            List<SbomGenerationRequest> generations,
            List<String> processedGenerationsIds,
            SbomGenerationStatus status,
            String prefix) {

        StringBuilder generationsSection = new StringBuilder();
        if (generations != null && !generations.isEmpty()) {
            for (SbomGenerationRequest generation : generations) {
                if (processedGenerationsIds.contains(generation.getId())) {
                    continue;
                }

                if (status.equals(generation.getStatus())) {
                    generationsSection.append("\n").append("https://").append(sbomerHost).append("/generations/").append(generation.getId());
                    processedGenerationsIds.add(generation.getId());
                }
            }
        }

        // If no generations were added, do not output anything in the comment
        if (!generationsSection.isEmpty()) {
            generationsSection.insert(0, prefix);
        }
        return generationsSection.toString();
    }

    private String generateRequestEventFinalSection(String requestEventId) {
        return "\n\nList of all manifests generated: " + "https://" + sbomerHost + "/requestevents/" + requestEventId
                + "\n";
    }

    private String getGenerationNVRFromManifest(V1Beta1RequestManifestRecord manifestRecord) {
        GenerationRequestType generationRequestType = GenerationRequestType
                .fromName(manifestRecord.generation().type());

        if (GenerationRequestType.BREW_RPM.equals(generationRequestType)) {
            // The NVR is stored as the generation identifier
            return manifestRecord.generation().identifier() + ": ";
        }

        if (GenerationRequestType.CONTAINERIMAGE.equals(generationRequestType)) {
            // The NVR is not stored inside the generation, we need to get it from the manifest. If it is null, it might
            // be a release manifest, we will return the identifier
            Sbom sbom = sbomService.get(manifestRecord.id());
            String[] nvrArray = SbomUtils.computeNVRFromContainerManifest(sbom.getSbom());
            if (nvrArray != null && nvrArray.length > 0) {
                return String.join("-", nvrArray);
            }
            return manifestRecord.identifier() + ": ";
        }

        // There are no NVRs associated with GenerationRequestType.BUILD or GenerationRequestType.OPERATION or
        // GenerationRequestType.ANALYSIS
        return "";
    }

    private boolean isValidRequestEvent(RequestEventStatusUpdateEvent event) {
        if (!isFinalStatus(event.getRequestEventStatus())) {
            log.warn("The request event is not a final expected status, ignoring the event!");
            return false;
        }
        if (event.getRequestEventConfig() == null) {
            log.warn("The event request config is null, ignoring the event!");
            return false;
        }
        if (!(event.getRequestEventConfig() instanceof ErrataAdvisoryRequestConfig)) {
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

    private Map<SbomGenerationStatus, Long> countGenerationsByStatus(List<SbomGenerationRequest> generations) {
        return generations.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.groupingBy(SbomGenerationRequest::getStatus, Collectors.counting()));
    }

}
