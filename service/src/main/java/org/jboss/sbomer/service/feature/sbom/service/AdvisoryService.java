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
package org.jboss.sbomer.service.feature.sbom.service;

import static org.jboss.sbomer.service.feature.sbom.errata.event.EventNotificationFiringUtil.notifyAdvisoryRelease;
import static org.jboss.sbomer.service.feature.sbom.errata.event.EventNotificationFiringUtil.notifyRequestEventStatusUpdate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.build.finder.koji.ClientSession;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.config.request.PncAnalysisRequestConfig;
import org.jboss.sbomer.core.config.request.PncBuildRequestConfig;
import org.jboss.sbomer.core.config.request.PncOperationRequestConfig;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.features.sbom.config.BrewRPMConfig;
import org.jboss.sbomer.core.features.sbom.config.DeliverableAnalysisConfig;
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventStatus;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataNotesSchemaValidator;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata.Details;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.Build;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.BuildItem;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataProduct;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus;
import org.jboss.sbomer.service.feature.sbom.errata.event.comment.RequestEventStatusUpdateEvent;
import org.jboss.sbomer.service.feature.sbom.errata.event.release.AdvisoryReleaseEvent;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;
import com.redhat.red.build.koji.model.xmlrpc.KojiIdOrName;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class AdvisoryService {

    @Inject
    @RestClient
    @Setter
    ErrataClient errataClient;

    @Inject
    @Setter
    ClientSession kojiSession;

    @Inject
    KubernetesClient kubernetesClient;

    @Inject
    @Setter
    SbomService sbomService;

    @Inject
    RequestEventRepository requestEventRepository;

    @Inject
    SbomGenerationRequestRepository generationRequestRepository;

    @Inject
    @Setter
    FeatureFlags featureFlags;

    @Inject
    ErrataNotesSchemaValidator notesSchemaValidator;

    public Collection<SbomGenerationRequest> generateFromAdvisory(RequestEvent requestEvent) {

        ErrataAdvisoryRequestConfig advisoryRequestConfig = (ErrataAdvisoryRequestConfig) requestEvent
                .getRequestConfig();

        // Fetching Erratum
        Errata erratum = errataClient.getErratum(advisoryRequestConfig.getAdvisoryId());
        if (erratum == null || erratum.getDetails().isEmpty()) {
            throw new ClientException(
                    "Could not retrieve the errata advisory with provided id {}",
                    advisoryRequestConfig.getAdvisoryId());
        }

        // Will be removed, leave now for debugging
        printAllErratumData(erratum);

        if (!erratum.getDetails().get().getTextonly()) {
            return handleStandardAdvisory(requestEvent, erratum);
        } else {
            return handleTextOnlyAdvisory(requestEvent, erratum);
        }
    }

    /*
     * Event will be ignored. Useful both for REST and UMB request events (won't leave the requestEvent as IN_PROGRESS)
     */
    protected Collection<SbomGenerationRequest> doIgnoreRequest(RequestEvent requestEvent, String reason) {
        log.debug(reason);
        updateRequest(requestEvent, RequestEventStatus.IGNORED, reason);
        return Collections.emptyList();
    }

    /*
     * Event will be marked as failed. Mostly useful for REST events (the AmqpMessageConsumer will catch the exception
     * and reupdate this request as FAILED)
     */
    protected Collection<SbomGenerationRequest> doFailRequest(RequestEvent requestEvent, String reason)
            throws ApplicationException {
        log.error(reason);
        updateRequest(requestEvent, RequestEventStatus.FAILED, reason);
        throw new ApplicationException(reason);
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    protected void updateRequest(RequestEvent requestEvent, RequestEventStatus status, String reason) {
        requestEventRepository.updateRequestEvent(requestEvent, status, Map.of(), reason);
    }

    private Collection<SbomGenerationRequest> handleTextOnlyAdvisory(RequestEvent requestEvent, Errata erratum) {

        if (!featureFlags.textOnlyErrataManifestGenerationEnabled()) {
            return doIgnoreRequest(requestEvent, "Text-Only Errata manifest generation is disabled");
        }

        Optional<JsonNode> maybeNotes = erratum.getNotesMapping();
        if (maybeNotes.isEmpty()) {
            String reason = String.format(
                    "Text-Only Errata Advisory '%s'(%s) does not have a Json-formatted notes field",
                    erratum.getDetails().get().getFulladvisory(),
                    erratum.getDetails().get().getId());

            return doIgnoreRequest(requestEvent, reason);
        }

        ValidationResult validationResult = notesSchemaValidator.validate(erratum);
        if (!validationResult.isValid()) {

            String reason = String.format(
                    "Text-Only Errata Advisory '%s'(%s) does not have a valid Json-formatted notes field",
                    erratum.getDetails().get().getFulladvisory(),
                    erratum.getDetails().get().getId());

            return doIgnoreRequest(requestEvent, reason);
        }

        List<RequestConfig> requestConfigsWithinNotes = new ArrayList<>();
        JsonNode notes = maybeNotes.get();

        if (notes.has("deliverables")) {
            JsonNode deliverables = notes.path("deliverables");

            if (deliverables.isArray()) {
                for (JsonNode deliverable : deliverables) {
                    String type = deliverable.path("type").asText();
                    log.debug("Processing deliverable of type: {}", type);

                    RequestConfig requestConfig = createRequestConfig(type, deliverable);
                    if (requestConfig != null) {
                        requestConfigsWithinNotes.add(requestConfig);
                    } else {
                        String reason = String.format(
                                "Unsupported deliverable type '%s' in Text-Only Errata Advisory '%s' (%s)",
                                type,
                                erratum.getDetails().get().getFulladvisory(),
                                erratum.getDetails().get().getId());

                        doFailRequest(requestEvent, reason);
                    }
                }
            }
        } else if (notes.has("manifest")) {

            log.debug(
                    "Text-Only Errata Advisory '{}'({}) has a \"manifest\" Notes field",
                    erratum.getDetails().get().getFulladvisory(),
                    erratum.getDetails().get().getId());

            // There is nothing to do for SBOMer, beside adding comments to the Errata
            updateRequest(requestEvent, RequestEventStatus.SUCCESS, null);

            // Send an async notification for the completed generations (will be used to add comments to Errata)
            notifyRequestEventStatusUpdate(
                    RequestEventStatusUpdateEvent.builder()
                            .withRequestEventConfig(
                                    ErrataAdvisoryRequestConfig.builder()
                                            .withAdvisoryId(String.valueOf(erratum.getDetails().get().getId()))
                                            .build())
                            .withRequestEventStatus(RequestEventStatus.SUCCESS)
                            .build());
        }

        return requestConfigsWithinNotes.isEmpty() ? Collections.emptyList()
                : processRequestConfigsWithinNotes(requestEvent, requestConfigsWithinNotes);
    }

    private Collection<SbomGenerationRequest> handleStandardAdvisory(RequestEvent requestEvent, Errata erratum) {
        log.info(
                "Advisory {} ({}) is standard (non Text-Only), with status {}",
                erratum.getDetails().get().getFulladvisory(),
                erratum.getDetails().get().getId(),
                erratum.getDetails().get().getStatus());

        Details details = erratum.getDetails().get();
        if (details.getContentTypes().isEmpty() || details.getContentTypes().size() > 1) {

            String reason = String.format(
                    "The standard errata advisory has zero or multiple content-types (%s)",
                    details.getContentTypes());
            doFailRequest(requestEvent, reason);
        }

        if (details.getContentTypes().stream().noneMatch(type -> type.equals("docker") || type.equals("rpm"))) {
            String reason = String
                    .format("The standard errata advisory has unknown content-types (%s)", details.getContentTypes());
            doFailRequest(requestEvent, reason);
        }

        ErrataBuildList erratumBuildList = errataClient.getBuildsList(String.valueOf(details.getId()));
        Map<ProductVersionEntry, List<BuildItem>> buildDetails = erratumBuildList.getProductVersions()
                .values()
                .stream()
                .collect(
                        Collectors.toMap(
                                productVersionEntry -> productVersionEntry,
                                productVersionEntry -> productVersionEntry.getBuilds()
                                        .stream()
                                        .flatMap(build -> build.getBuildItems().values().stream())
                                        .collect(Collectors.toList())));

        // If the status is SHIPPED_LIVE and there is a successful generation for this advisory, create release
        // manifests. Otherwise SBOMer will default to the creation of build manifests. Will change in future!
        V1Beta1RequestRecord successfulRequestRecord = null;
        if (ErrataStatus.SHIPPED_LIVE.equals(details.getStatus())) {
            successfulRequestRecord = sbomService
                    .searchLastSuccessfulAdvisoryRequestRecord(String.valueOf(erratum.getDetails().get().getId()));
        }

        if (details.getContentTypes().contains("docker")) {
            if (successfulRequestRecord == null) {
                return createBuildManifestsForDockerBuilds(requestEvent, buildDetails);
            } else {
                return createReleaseManifestsForBuildsOfType(
                        erratum,
                        requestEvent,
                        buildDetails,
                        successfulRequestRecord,
                        GenerationRequestType.CONTAINERIMAGE);
            }
        } else {
            if (successfulRequestRecord == null) {
                ErrataProduct product = errataClient.getProduct(details.getProduct().getShortName());
                return createBuildManifestsForRPMBuilds(requestEvent, details, product, buildDetails);
            } else {
                return createReleaseManifestsForBuildsOfType(
                        erratum,
                        requestEvent,
                        buildDetails,
                        successfulRequestRecord,
                        GenerationRequestType.BREW_RPM);
            }
        }
    }

    @Transactional
    protected Collection<SbomGenerationRequest> processRequestConfigsWithinNotes(
            RequestEvent requestEvent,
            List<RequestConfig> requestConfigsWithinNotes) {

        List<SbomGenerationRequest> generations = new ArrayList<SbomGenerationRequest>();
        for (RequestConfig config : requestConfigsWithinNotes) {
            if (config instanceof PncBuildRequestConfig) {
                log.info("New PNC build request received");

                generations.add(sbomService.generateFromBuild(requestEvent, config, null));
            } else if (config instanceof PncAnalysisRequestConfig analysisConfig) {
                log.info("New PNC analysis request received");

                generations.add(
                        sbomService.generateNewOperation(
                                requestEvent,
                                DeliverableAnalysisConfig.builder()
                                        .withDeliverableUrls(analysisConfig.getUrls())
                                        .withMilestoneId(analysisConfig.getMilestoneId())
                                        .build()));
            } else if (config instanceof PncOperationRequestConfig operationConfig) {
                log.info("New PNC operation request received");

                generations.add(
                        sbomService.generateFromOperation(
                                requestEvent,
                                OperationConfig.builder().withOperationId(operationConfig.getOperationId()).build()));
            }
        }

        return generations;
    }

    /**
     * Helper method to create RequestConfig based on the deliverable type.
     */
    private RequestConfig createRequestConfig(String type, JsonNode deliverableEntry) {
        try {
            switch (type) {
                case PncBuildRequestConfig.TYPE_NAME:
                    return ObjectMapperProvider.json().treeToValue(deliverableEntry, PncBuildRequestConfig.class);
                case PncOperationRequestConfig.TYPE_NAME:
                    return ObjectMapperProvider.json().treeToValue(deliverableEntry, PncOperationRequestConfig.class);
                case PncAnalysisRequestConfig.TYPE_NAME:
                    return ObjectMapperProvider.json().treeToValue(deliverableEntry, PncAnalysisRequestConfig.class);
                default:
                    return null;
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            log.error("Failed to deserialize deliverable of type '{}': {}", type, e.getMessage());
            return null;
        }
    }

    @Transactional
    protected Collection<SbomGenerationRequest> createBuildManifestsForDockerBuilds(
            RequestEvent requestEvent,
            Map<ProductVersionEntry, List<BuildItem>> buildDetails) {

        if (!featureFlags.standardErrataImageManifestGenerationEnabled()) {
            return doIgnoreRequest(requestEvent, "Standard Errata container images manifest generation is disabled");
        }

        log.debug("Creating build manifests for Docker builds: {}", buildDetails);

        Collection<SbomGenerationRequest> sbomRequests = new ArrayList<SbomGenerationRequest>();

        // Collect all the docker build ids so we can query Koji in one go
        List<Long> buildIds = buildDetails.values()
                .stream()
                .flatMap(List::stream)
                .map(BuildItem::getId)
                .collect(Collectors.toList());

        Map<Long, String> imageNamesFromBuilds = getImageNamesFromBuilds(buildIds);
        imageNamesFromBuilds.forEach((buildId, imageName) -> {
            log.debug("Retrieved imageName '{}' for buildId {}", imageName, buildId);
            if (imageName != null) {
                SyftImageConfig config = SyftImageConfig.builder().withIncludeRpms(true).withImage(imageName).build();
                log.debug("Creating GenerationRequest Kubernetes resource...");
                sbomRequests.add(sbomService.generateSyftImage(requestEvent, config));
            }
        });

        return sbomRequests;
    }

    @Transactional
    protected Map<ProductVersionEntry, SbomGenerationRequest> createReleaseManifestsGenerationsForType(
            Errata erratum,
            RequestEvent requestEvent,
            Map<ProductVersionEntry, List<BuildItem>> buildDetails,
            GenerationRequestType type) {

        // We need to create 1 release manifest per ProductVersion
        // We will identify the Generation with the {Errata}#{ProductVersion} identifier
        Map<ProductVersionEntry, SbomGenerationRequest> pvToGenerations = new HashMap<ProductVersionEntry, SbomGenerationRequest>();
        buildDetails.keySet().forEach(pv -> {
            SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.builder()
                    .withId(RandomStringIdGenerator.generate())
                    .withIdentifier(erratum.getDetails().get().getFulladvisory() + "#" + pv.getName())
                    .withType(type)
                    .withStatus(SbomGenerationStatus.GENERATING)
                    .withConfig(null) // I really don't know what to put here
                    .withRequest(requestEvent)
                    .build();

            pvToGenerations.put(pv, generationRequestRepository.save(sbomGenerationRequest));
        });
        return pvToGenerations;
    }

    protected Collection<SbomGenerationRequest> createReleaseManifestsForBuildsOfType(
            Errata erratum,
            RequestEvent requestEvent,
            Map<ProductVersionEntry, List<BuildItem>> buildDetails,
            V1Beta1RequestRecord successfulAdvisoryRequestRecord,
            GenerationRequestType type) {

        if (!featureFlags.standardErrataImageManifestGenerationEnabled()) {
            return doIgnoreRequest(requestEvent, "Standard Errata container images manifest generation is disabled");
        }

        Map<ProductVersionEntry, SbomGenerationRequest> releaseGenerations = createReleaseManifestsGenerationsForType(
                erratum,
                requestEvent,
                buildDetails,
                type);

        // Send an async notification for the completed generations (will be used to add comments to Errata)
        notifyAdvisoryRelease(
                AdvisoryReleaseEvent.builder()
                        .withRequestEventId(requestEvent.getId())
                        .withReleaseGenerations(releaseGenerations)
                        .build());

        return releaseGenerations.values();
    }

    @Transactional
    protected Collection<SbomGenerationRequest> createBuildManifestsForRPMBuilds(
            RequestEvent requestEvent,
            Details details,
            ErrataProduct product,
            Map<ProductVersionEntry, List<BuildItem>> buildDetails) {

        if (!featureFlags.standardErrataRPMManifestGenerationEnabled()) {
            return doIgnoreRequest(requestEvent, "Standard Errata RPM manifest generation is disabled");
        }

        log.debug("Creating build manifests for RPM builds: {}", buildDetails);

        Collection<SbomGenerationRequest> sbomRequests = new ArrayList<SbomGenerationRequest>();

        buildDetails.forEach((pVersion, items) -> {
            String productName = product.getData().getAttributes().getName();
            log.debug("Processing RPM builds of Errata Product '{}' with Product Version: '{}'", productName, pVersion);

            if (items == null || items.isEmpty()) {
                log.warn(
                        "No RPM builds associated with Errata {} and Product Version: '{}'",
                        details.getId(),
                        pVersion);
                return;
            }

            Long pVersionId = findErrataProductVersionIdByName(product, pVersion.getName());

            items.forEach(item -> {
                BrewRPMConfig config = BrewRPMConfig.builder()
                        .withAdvisoryId(details.getId())
                        .withAdvisory(details.getFulladvisory())
                        .withProductVersionId(pVersionId)
                        .withProductVersion(pVersion.getName())
                        .withBrewBuildId(item.getId())
                        .withBrewBuildNVR(item.getNvr())
                        .build();

                log.debug("Creating GenerationRequest Kubernetes resource...");

                GenerationRequest req = new GenerationRequestBuilder(GenerationRequestType.BREW_RPM)
                        .withIdentifier(config.getBrewBuildNVR())
                        .withStatus(SbomGenerationStatus.NEW)
                        .withConfig(config)
                        .build();

                log.debug("ConfigMap to create: '{}'", req);

                SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.sync(requestEvent, req);
                kubernetesClient.configMaps().resource(req).create();

                sbomRequests.add(sbomGenerationRequest);
            });
        });

        return sbomRequests;
    }

    private Long findErrataProductVersionIdByName(ErrataProduct product, String pVersionName) {
        if (product != null && product.getData() != null && product.getData().getRelationships() != null) {
            return product.getData()
                    .getRelationships()
                    .getProductVersions()
                    .stream()
                    .filter(version -> version.getName().equals(pVersionName))
                    .map(ErrataProduct.ErrataProductVersion::getId)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private Map<Long, String> getImageNamesFromBuilds(List<Long> buildIds) {
        Map<Long, String> buildsToImageName = new HashMap<Long, String>();

        try {
            List<KojiIdOrName> kojiIdOrNames = buildIds.stream()
                    .map(id -> KojiIdOrName.getFor(id.toString()))
                    .collect(Collectors.toList());

            List<KojiBuildInfo> buildInfos = kojiSession.getBuild(kojiIdOrNames);
            for (KojiBuildInfo kojiBuildInfo : buildInfos) {
                String imageName = Optional.ofNullable(kojiBuildInfo.getExtra())
                        .map(extra -> (Map<String, Object>) extra.get("image"))
                        .map(imageMap -> (Map<String, Object>) imageMap.get("index"))
                        .map(indexMap -> (List<String>) indexMap.get("pull"))
                        .flatMap(
                                list -> list != null && !list.isEmpty()
                                        ? list.stream().filter(item -> item.contains("sha256")).findFirst()
                                        : Optional.empty())
                        .or(
                                () -> Optional.ofNullable(kojiBuildInfo.getExtra())
                                        .map(extra -> (Map<String, Object>) extra.get("image"))
                                        .map(imageMap -> (Map<String, Object>) imageMap.get("index"))
                                        .map(indexMap -> (List<String>) indexMap.get("pull"))
                                        .flatMap(
                                                list -> list != null && !list.isEmpty() ? list.stream().findFirst()
                                                        : Optional.empty()))
                        .orElse(null);
                buildsToImageName.put(Long.valueOf(kojiBuildInfo.getId()), imageName);
            }
        } catch (KojiClientException e) {
            log.error("Unable to fetch containers information for buildIDs: {}", buildIds, e);
            return null;
        }
        return buildsToImageName;
    }

    private void printAllErratumData(Errata erratum) {

        Optional<JsonNode> notes = erratum.getNotesMapping();

        if (notes.isEmpty()) {
            log.info("The erratum does not contain any JSON content inside the notes...");
        } else {
            log.info("The erratum contains a notes content with following JSON: \n{}", notes.get().toPrettyString());
        }

        if (erratum.getDetails().isEmpty()) {
            log.warn("Mmmmm I don't know how to get the release information...");
            return;
        }

        log.info("Fetching Erratum release ...");
        ErrataRelease erratumRelease = errataClient.getRelease(String.valueOf(erratum.getDetails().get().getGroupId()));

        log.info("Fetching Erratum builds list ...");
        ErrataBuildList erratumBuildList = errataClient
                .getBuildsList(String.valueOf(erratum.getDetails().get().getId()));

        StringBuilder summary = new StringBuilder("\n**********************************\n");
        summary.append("ID: ").append(erratum.getDetails().get().getId());
        summary.append("\nTYPE: ").append(erratum.getOriginalType());
        summary.append("\nAdvisory: ").append(erratum.getDetails().get().getFulladvisory());
        summary.append("\nSynopsis: ").append(erratum.getDetails().get().getSynopsis());
        summary.append("\nStatus: ").append(erratum.getDetails().get().getStatus());
        summary.append("\nCVE: ").append(erratum.getContent().getContent().getCve());
        summary.append("\n\nProduct: ")
                .append(erratum.getDetails().get().getProduct().getName())
                .append("(")
                .append(erratum.getDetails().get().getProduct().getShortName())
                .append(")");
        summary.append("\nRelease: ").append(erratumRelease.getData().getAttributes().getName());
        summary.append("\n\nBuilds: ");
        if (erratumBuildList != null) {
            if (erratumBuildList.getProductVersions() != null && erratumBuildList.getProductVersions().size() > 0) {
                for (ProductVersionEntry productVersionEntry : erratumBuildList.getProductVersions().values()) {
                    summary.append("\n\tProduct Version: ").append(productVersionEntry.getName());
                    for (Build build : productVersionEntry.getBuilds()) {

                        summary.append("\n\t\t").append(build.getBuildItems().values().stream().map(buildItem -> {
                            return "ID: " + buildItem.getId() + ", NVR: " + buildItem.getNvr() + ", Variant: "
                                    + buildItem.getVariantArch().keySet();
                        }).collect(Collectors.joining("\n\t\t")));
                    }
                }
            }
        }
        if (notes.isPresent()) {
            summary.append("\nJSON Notes:\n").append(notes.get().toPrettyString());
        } else {
            if (erratum.getContent().getContent().getNotes() != null
                    && erratum.getContent().getContent().getNotes().trim().length() > 0) {
                summary.append("\nNotes:\n").append(erratum.getContent().getContent().getNotes());
            }
        }

        summary.append("\n**********************************\n");
        System.out.println(summary.toString());
    }

}
