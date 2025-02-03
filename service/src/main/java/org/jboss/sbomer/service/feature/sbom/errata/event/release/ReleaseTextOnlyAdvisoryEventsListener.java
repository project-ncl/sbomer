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
package org.jboss.sbomer.service.feature.sbom.errata.event.release;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.component.evidence.Identity.Field;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestManifestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.Constants;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventStatus;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.event.AdvisoryEventUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;
import org.jboss.sbomer.service.feature.sbom.service.SbomGenerationRequestRepository;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.stats.StatsService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ReleaseTextOnlyAdvisoryEventsListener {

    // Set the long transaction imeout to 10 mins
    private static final int INCREASED_TIMEOUT_SEC = 600;

    @Inject
    @RestClient
    @Setter
    ErrataClient errataClient;

    @Inject
    @Setter
    SbomService sbomService;

    @Inject
    @Setter
    StatsService statsService;

    @Inject
    @Setter
    SbomGenerationRequestRepository generationRequestRepository;

    @Inject
    @Setter
    RequestEventRepository requestEventRepository;

    public void onReleaseAdvisoryEvent(@ObservesAsync TextOnlyAdvisoryReleaseEvent event) {
        log.debug("Event received for text-only advisory release ...");

        RequestEvent requestEvent = requestEventRepository.findById(event.getRequestEventId());
        try {
            ErrataAdvisoryRequestConfig config = (ErrataAdvisoryRequestConfig) requestEvent.getRequestConfig();
            Errata erratum = errataClient.getErratum(config.getAdvisoryId());
            String toolVersion = statsService.getStats().getVersion();
            Component.Type productType = AdvisoryEventUtils
                    .getComponentTypeForProduct(erratum.getDetails().get().getProduct().getShortName());

            JsonNode notes = erratum.getNotesMapping().get();
            List<String> manifestsPurls = null;

            if (notes.has("manifest")) {
                log.debug(
                        "Creating release manifests for manual builds of advisory: '{}'[{}]",
                        erratum.getDetails().get().getFulladvisory(),
                        erratum.getDetails().get().getId());

                // If the notes contains a "manifest" field, search the successful generations for all the purls listed
                // (there are no generations associated to the requestevent because no generations were triggered)
                manifestsPurls = AdvisoryEventUtils.extractPurlUrisFromManifestNode(notes);
            } else {
                log.debug(
                        "Creating release manifests for managed builds of advisory: '{}'[{}]",
                        erratum.getDetails().get().getFulladvisory(),
                        erratum.getDetails().get().getId());

                // If the notes contains a "deliverables" field, search the latest successful generations triggered by
                // the request event
                V1Beta1RequestRecord advisoryManifestsRecord = sbomService
                        .searchLastSuccessfulAdvisoryRequestRecord(requestEvent.getId(), config.getAdvisoryId());
                manifestsPurls = advisoryManifestsRecord.manifests()
                        .stream()
                        .map(V1Beta1RequestManifestRecord::rootPurl)
                        .toList();
            }

            List<Sbom> sboms = findSbomsByPurls(manifestsPurls);
            if (sboms.isEmpty()) {
                markRequestFailed(
                        requestEvent,
                        event.getReleaseGenerations().values(),
                        "There are no matching sboms for the content specified within the advisory notes.");
                doUpdateGenerationsStatus(event.getReleaseGenerations().values());
                return;
            }
            createReleaseManifestsForTextOnlyAdvisories(
                    requestEvent,
                    erratum,
                    sboms,
                    event.getReleaseGenerations(),
                    toolVersion,
                    productType);
        } catch (Exception e) {
            log.error(
                    "An error occured during the creation of release manifests for event '{}'",
                    requestEvent.getId(),
                    e);
            markRequestFailed(
                    requestEvent,
                    event.getReleaseGenerations().values(),
                    "An error occured during the creation of the release manifest");
        }

        // Let's trigger the update of statuses and advisory comments
        doUpdateGenerationsStatus(event.getReleaseGenerations().values());
    }

    private List<Sbom> findSbomsByPurls(List<String> purls) {
        if (purls == null || purls.isEmpty()) {
            return Collections.emptyList();
        }
        return purls.stream().map(sbomService::findByPurl).filter(Objects::nonNull).toList();
    }

    @Transactional(value = TxType.REQUIRES_NEW)
    protected void markRequestFailed(
            RequestEvent requestEvent,
            Collection<SbomGenerationRequest> releaseGenerations,
            String reason) {
        log.error(reason);

        requestEvent = requestEventRepository.findById(requestEvent.getId());
        requestEvent.setEventStatus(RequestEventStatus.FAILED);
        requestEvent.setReason(reason);

        for (SbomGenerationRequest generation : releaseGenerations) {
            generation = generationRequestRepository.findById(generation.getId());
            generation.setStatus(SbomGenerationStatus.FAILED);
            generation.setReason(reason);
        }
    }

    protected void createReleaseManifestsForTextOnlyAdvisories(
            RequestEvent requestEvent,
            Errata erratum,
            List<Sbom> sboms,
            Map<String, SbomGenerationRequest> releaseGenerations,
            String toolVersion,
            Component.Type productType) {

        String productName = erratum.getDetails().get().getProduct().getName();
        String productVersion = erratum.getContent().getContent().getProductVersionText();

        // Create the release manifest for this ProductVersion
        Bom productVersionBom = createProductVersionBom(productType, erratum, toolVersion);
        for (Sbom sbom : sboms) {
            Component sbomRootComponent = createRootComponentForSbom(sbom);
            // Add the component to the release manifest components and add the purl to the "provides" list
            productVersionBom.addComponent(sbomRootComponent);
            productVersionBom.getDependencies().get(0).addProvides(new Dependency(sbomRootComponent.getPurl()));
        }

        SbomUtils.addMissingSerialNumber(productVersionBom);

        SbomGenerationRequest releaseGeneration = releaseGenerations.get(productVersion);
        Sbom sbom = saveReleaseManifestForTextOnlyAdvisories(
                requestEvent,
                erratum,
                productName,
                productVersion,
                toolVersion,
                releaseGeneration,
                productVersionBom,
                sboms);

        log.info(
                "Saved and modified SBOM '{}' for generation '{}' for ProductVersion '{}' of errata '{}'",
                sbom,
                releaseGeneration.getId(),
                productVersion,
                erratum.getDetails().get().getFulladvisory());
    }

    @Transactional
    protected void doUpdateGenerationsStatus(Collection<SbomGenerationRequest> releaseGenerations) {
        // Update only one SbomGenerationRequest, because the requestEvent associated is the same for all of them. This
        // avoids duplicated comments in the advisory
        if (releaseGenerations != null && !releaseGenerations.isEmpty()) {
            SbomGenerationRequest generation = releaseGenerations.iterator().next();
            generation = generationRequestRepository.findById(generation.getId());
            SbomGenerationRequest.updateRequestEventStatus(generation);
        }
    }

    private Bom createProductVersionBom(Component.Type productType, Errata erratum, String toolVersion) {

        String productName = erratum.getDetails().get().getProduct().getName();
        String productVersion = erratum.getContent().getContent().getProductVersionText();
        String cpe = erratum.getContent().getContent().getTextOnlyCpe();

        // Create the release manifest for this ProductVersion
        Bom bom = SbomUtils.createBom();
        Metadata metadata = createMetadata(
                productName,
                productVersion,
                productType,
                Set.of(cpe),
                erratum.getDetails().get().getActualShipDate() != null
                        ? Date.from(erratum.getDetails().get().getActualShipDate())
                        : null,
                toolVersion);
        bom.setMetadata(metadata);
        Dependency mainDependency = new Dependency(metadata.getComponent().getBomRef());
        bom.setDependencies(List.of(mainDependency));
        return bom;
    }

    protected Component createRootComponentForSbom(Sbom sbom) {

        Bom manifestBom = SbomUtils.fromJsonNode(sbom.getSbom());
        Component manifestMainComponent = null;
        Component metadataComponent = manifestBom.getMetadata().getComponent();
        // If the are no components or the manifest is a ZIP manifest, get the main component from the metadata
        if (manifestBom.getComponents() == null || manifestBom.getComponents().isEmpty()
                || SbomUtils.hasProperty(metadataComponent, "deliverable-url")) {
            manifestMainComponent = metadataComponent;
        } else {
            manifestMainComponent = manifestBom.getComponents().get(0);
        }
        String evidencePurl = SbomUtils.addQualifiersToPurlOfComponent(
                manifestMainComponent,
                Map.of("repository_url", Constants.MRRC_URL),
                !SbomUtils.hasProperty(manifestMainComponent, "deliverable-url"));

        // Finally create the root component for this build (NVR) from the manifest
        Component sbomRootComponent = SbomUtils.createComponent(manifestMainComponent);

        sbomRootComponent.setSupplier(manifestMainComponent.getSupplier());
        sbomRootComponent.setPublisher(manifestMainComponent.getPublisher());
        sbomRootComponent.setHashes(manifestMainComponent.getHashes());
        sbomRootComponent.setLicenses(manifestMainComponent.getLicenses());
        SbomUtils.setEvidenceIdentities(sbomRootComponent, Set.of(evidencePurl), Field.PURL);

        return sbomRootComponent;
    }

    // Add a very long timeout because this method could potentially need to update hundreds of manifests
    @Retry(maxRetries = 10)
    protected Sbom saveReleaseManifestForTextOnlyAdvisories(
            RequestEvent requestEvent,
            Errata erratum,
            String productName,
            String productVersion,
            String toolVersion,
            SbomGenerationRequest releaseGeneration,
            Bom productVersionBom,
            List<Sbom> sboms) {

        try {
            QuarkusTransaction.begin(QuarkusTransaction.beginOptions().timeout(INCREASED_TIMEOUT_SEC));

            // 1 - Save the release generation with the release manifest
            releaseGeneration = generationRequestRepository.findById(releaseGeneration.getId());
            releaseGeneration.setStatus(SbomGenerationStatus.FINISHED);
            releaseGeneration.setResult(GenerationResult.SUCCESS);

            // 1.1 - Create the Sbom entity
            Sbom releaseSbom = Sbom.builder()
                    .withIdentifier(releaseGeneration.getIdentifier())
                    .withSbom(SbomUtils.toJsonNode(productVersionBom))
                    .withGenerationRequest(releaseGeneration)
                    .withConfigIndex(0)
                    .build();

            // Add more information for this release so to find manifests more easily
            ObjectNode metadataNode = collectReleaseInfo(
                    requestEvent.getId(),
                    erratum,
                    productName,
                    productVersion,
                    toolVersion,
                    productVersionBom);
            releaseSbom.setReleaseMetadata(metadataNode);
            releaseSbom = sbomService.save(releaseSbom);

            // 2 - For every sbom update it with the release repo
            log.debug("Processing {} sboms...", sboms.size());
            for (Sbom sbom : sboms) {
                log.debug("Updating sbom {} for release event {}...", sbom.getId(), requestEvent.getId());

                Sbom buildManifest = sbomService.get(sbom.getId());
                Bom manifestBom = SbomUtils.fromJsonNode(buildManifest.getSbom());

                Component metadataComponent = manifestBom.getMetadata() != null
                        ? manifestBom.getMetadata().getComponent()
                        : null;
                if (metadataComponent != null) {
                    adjustComponent(metadataComponent);
                }
                for (Component component : manifestBom.getComponents()) {
                    adjustComponent(component);
                }

                // 2.7 - Update the original Sbom
                buildManifest.setSbom(SbomUtils.toJsonNode(manifestBom));

                // 2.8 - Add more information for this release so to find manifests more easily
                ObjectNode buildManifestMetadataNode = collectReleaseInfo(
                        requestEvent.getId(),
                        erratum,
                        productName,
                        productVersion,
                        toolVersion,
                        manifestBom);
                buildManifest.setReleaseMetadata(buildManifestMetadataNode);
            }

            requestEvent = requestEventRepository.findById(requestEvent.getId());
            requestEvent.setEventStatus(RequestEventStatus.SUCCESS);
            QuarkusTransaction.commit();

            return releaseSbom;
        } catch (Exception e) {
            try {
                QuarkusTransaction.rollback();
            } catch (Exception rollbackException) {
                log.error("Transaction was rolled back!!", rollbackException);
            }
            throw new ApplicationException(
                    "Could not save the release and build manifests for release generation {}",
                    releaseGeneration.getIdentifier(),
                    e);
        }
    }

    public static final String REQUEST_ID = "request_id";
    public static final String ERRATA = "errata_fullname";
    public static final String ERRATA_ID = "errata_id";
    public static final String ERRATA_SHIP_DATE = "errata_ship_date";
    public static final String PRODUCT = "product_name";
    public static final String PRODUCT_SHORTNAME = "product_shortname";
    public static final String PRODUCT_VERSION = "product_version";
    public static final String PURL_LIST = "purl_list";

    protected ObjectNode collectReleaseInfo(
            String requestEventId,
            Errata erratum,
            String product,
            String productVersion,
            String toolVersion,
            Bom manifest) {

        ObjectNode releaseMetadata = ObjectMapperProvider.json().createObjectNode();
        releaseMetadata.put(REQUEST_ID, requestEventId);
        releaseMetadata.put(ERRATA, erratum.getDetails().get().getFulladvisory());
        releaseMetadata.put(ERRATA_ID, erratum.getDetails().get().getId());
        if (erratum.getDetails().get().getActualShipDate() != null) {
            releaseMetadata.put(ERRATA_SHIP_DATE, Date.from(erratum.getDetails().get().getActualShipDate()).toString());
        }
        releaseMetadata.put(PRODUCT, product);
        releaseMetadata.put(PRODUCT_SHORTNAME, erratum.getDetails().get().getProduct().getShortName());
        releaseMetadata.put(PRODUCT_VERSION, productVersion);

        TreeSet<String> allPurls = new TreeSet<>();
        if (manifest.getMetadata() != null) {
            allPurls.addAll(SbomUtils.getAllPurlsOfComponent(manifest.getMetadata().getComponent()));
        }
        for (Component component : manifest.getComponents()) {
            allPurls.addAll(SbomUtils.getAllPurlsOfComponent(component));
        }
        ArrayNode purlArray = ObjectMapperProvider.json().createArrayNode();
        for (String purl : allPurls) {
            purlArray.add(purl);
        }
        releaseMetadata.set(PURL_LIST, purlArray);
        return releaseMetadata;
    }

    private void adjustComponent(Component component) {

        String evidencePurl = SbomUtils.addQualifiersToPurlOfComponent(
                component,
                Map.of("repository_url", Constants.MRRC_URL),
                !SbomUtils.hasProperty(component, "deliverable-url"));
        log.debug("Calculated evidence purl: {}", evidencePurl);
        component.setPurl(evidencePurl);
        SbomUtils.setEvidenceIdentities(component, Set.of(evidencePurl), Field.PURL);
    }

    private Metadata createMetadata(
            String name,
            String version,
            Component.Type type,
            Set<String> cpes,
            Date shipDate,
            String toolVersion) {
        Metadata metadata = new Metadata();

        Component metadataProductComponent = SbomUtils.createComponent(null, name, version, null, null, type);
        metadataProductComponent.setBomRef(version);
        SbomUtils.setSupplier(metadataProductComponent);
        SbomUtils.setEvidenceIdentities(metadataProductComponent, cpes, Field.CPE);

        metadata.setComponent(metadataProductComponent);
        if (shipDate != null) {
            metadata.setTimestamp(shipDate);
        }
        metadata.setToolChoice(SbomUtils.createToolInformation(toolVersion));
        return metadata;
    }

}
