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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.component.evidence.Identity.Field;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1GenerationRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestManifestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata.Details;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.BuildItem;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.feature.sbom.errata.event.AdvisoryEventUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.pyxis.PyxisClient;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepository;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepositoryDetails;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.RepositoryCoordinates;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;
import org.jboss.sbomer.service.feature.sbom.service.SbomGenerationRequestRepository;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.stats.StatsService;

import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ReleaseAdvisoryEventsListener {

    // Set the long transaction imeout to 10 mins
    private static final int INCREASED_TIMEOUT_SEC = 600;

    @Inject
    @RestClient
    @Setter
    ErrataClient errataClient;

    @Inject
    @RestClient
    @Setter
    PyxisClient pyxisClient;

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

    private static final String NVR_STANDARD_SEPARATOR = "-";

    public void onReleaseAdvisoryEvent(@ObservesAsync AdvisoryReleaseEvent event) {
        log.debug("Event received for advisory release ...");

        RequestEvent requestEvent = requestEventRepository.findById(event.getRequestEventId());

        ErrataAdvisoryRequestConfig advisoryRequestConfig = (ErrataAdvisoryRequestConfig) requestEvent
                .getRequestConfig();

        // Fetching Erratum
        Errata erratum = errataClient.getErratum(advisoryRequestConfig.getAdvisoryId());
        Map<ProductVersionEntry, List<BuildItem>> advisoryBuildDetails = getAdvisoryBuildDetails(
                advisoryRequestConfig.getAdvisoryId());
        V1Beta1RequestRecord advisoryManifestsRecord = sbomService
                .searchLastSuccessfulAdvisoryRequestRecord(advisoryRequestConfig.getAdvisoryId());

        if (erratum.getDetails().get().getContentTypes().contains("docker")) {

            log.debug(
                    "Creating release manifests for Docker builds of advisory: '{}'[{}]",
                    erratum.getDetails().get().getFulladvisory(),
                    erratum.getDetails().get().getId());

            releaseManifestsForDockerBuilds(
                    erratum,
                    advisoryBuildDetails,
                    advisoryManifestsRecord,
                    event.getReleaseGenerations());
        } else {

            log.debug(
                    "Creating release manifests for RPM builds of advisory: '{}'[{}]",
                    erratum.getDetails().get().getFulladvisory(),
                    erratum.getDetails().get().getId());

            releaseManifestsForRPMBuilds();
        }
    }

    @Transactional
    protected Map<ProductVersionEntry, List<BuildItem>> getAdvisoryBuildDetails(String advisoryId) {
        ErrataBuildList erratumBuildList = errataClient.getBuildsList(advisoryId);
        Map<ProductVersionEntry, List<BuildItem>> advisoryBuildDetails = erratumBuildList.getProductVersions()
                .values()
                .stream()
                .collect(
                        Collectors.toMap(
                                productVersionEntry -> productVersionEntry,
                                productVersionEntry -> productVersionEntry.getBuilds()
                                        .stream()
                                        .flatMap(build -> build.getBuildItems().values().stream())
                                        .collect(Collectors.toList())));
        return advisoryBuildDetails;
    }

    protected void releaseManifestsForRPMBuilds() {
        throw new ApplicationException("**** NOT IMPLEMENTED ****");
    }

    protected void releaseManifestsForDockerBuilds(
            Errata erratum,
            Map<ProductVersionEntry, List<BuildItem>> advisoryBuildDetails,
            V1Beta1RequestRecord advisoryManifestsRecord,
            Map<ProductVersionEntry, SbomGenerationRequest> releaseGenerations) {

        String toolVersion = statsService.getStats().getVersion();
        Component.Type productType = AdvisoryEventUtils
                .getComponentTypeForProduct(erratum.getDetails().get().getProduct().getShortName());

        // Associate each build (NVR) in an advisory to its build manifest generation
        Map<String, V1Beta1GenerationRecord> nvrToBuildGeneration = mapNVRToBuildGeneration(advisoryManifestsRecord);

        // Associate each ProductVersion to its list of CPEs
        Map<ProductVersionEntry, Set<String>> productVersionToCPEs = mapProductVersionToCPEs(advisoryBuildDetails);

        advisoryBuildDetails.forEach((productVersion, buildItems) -> {
            // Create the release manifest for this ProductVersion
            Bom productVersionBom = SbomUtils.createBom();
            Metadata productVersionMetadata = createMetadata(
                    productVersion.getDescription(),
                    productVersion.getName(),
                    productType,
                    productVersionToCPEs.get(productVersion),
                    Date.from(erratum.getDetails().get().getActualShipDate()),
                    toolVersion);
            productVersionBom.setMetadata(productVersionMetadata);
            Dependency productVersionDependency = new Dependency(productVersionMetadata.getComponent().getBomRef());
            productVersionBom.setDependencies(List.of(productVersionDependency));

            // Associate each build (NVR == generation) in an advisory to the repositories where it is published to
            Map<String, List<RepositoryCoordinates>> generationToRepositories = new HashMap<String, List<RepositoryCoordinates>>();

            for (BuildItem buildItem : buildItems) {
                Component nvrRootComponent = createRootComponentForBuildItem(
                        buildItem.getNvr(),
                        nvrToBuildGeneration.get(buildItem.getNvr()),
                        advisoryManifestsRecord,
                        generationToRepositories);

                // Add the component to the release manifest components and add the purl to the "provides" list
                productVersionBom.addComponent(nvrRootComponent);
                productVersionBom.getDependencies().get(0).addProvides(new Dependency(nvrRootComponent.getPurl()));
            }

            SbomUtils.addMissingSerialNumber(productVersionBom);

            SbomGenerationRequest releaseGeneration = releaseGenerations.get(productVersion);
            Sbom sbom = saveReleaseManifestForGeneration(
                    releaseGeneration,
                    productVersionBom,
                    advisoryManifestsRecord,
                    generationToRepositories);

            log.info(
                    "Saved and modified SBOM '{}' for generation '{}' for ProductVersion '{}' of errata '{}'",
                    sbom,
                    releaseGeneration.getId(),
                    productVersion.getName(),
                    erratum.getDetails().get().getFulladvisory());
        });
    }

    protected Component createRootComponentForBuildItem(
            String generationNVR,
            V1Beta1GenerationRecord generation,
            V1Beta1RequestRecord advisoryManifestsRecord,
            Map<String, List<RepositoryCoordinates>> generationToRepositories) {

        // From the generation triggered from this build (NVR), find the image index manifest and get the manifest
        // content, we need to copy the main component
        V1Beta1RequestManifestRecord imageIndexManifest = findImageIndexManifest(advisoryManifestsRecord, generation);
        Sbom imageIndexSbom = sbomService.get(imageIndexManifest.id());
        Component imageIndexMainComponent = SbomUtils.fromJsonNode(imageIndexSbom.getSbom()).getComponents().get(0);

        // Find where this build (NVR) has been published to
        List<RepositoryCoordinates> repositories = getRepositoriesDetails(generationNVR);

        // Create summary (pick the longest value) and evidence purl
        RepositoryCoordinates preferredRepo = AdvisoryEventUtils.findPreferredRepo(repositories);
        String summaryPurl = AdvisoryEventUtils.createPurl(preferredRepo, imageIndexMainComponent.getVersion(), false);
        Set<String> evidencePurls = AdvisoryEventUtils
                .createPurls(repositories, imageIndexMainComponent.getVersion(), true);

        // Finally create the root component for this build (NVR) from the image index manifest
        Component nvrRootComponent = SbomUtils.createComponent(
                null,
                imageIndexMainComponent.getName(),
                imageIndexMainComponent.getVersion(),
                null,
                summaryPurl,
                imageIndexMainComponent.getType());

        nvrRootComponent.setSupplier(imageIndexMainComponent.getSupplier());
        nvrRootComponent.setPublisher(imageIndexMainComponent.getPublisher());
        nvrRootComponent.setHashes(imageIndexMainComponent.getHashes());
        nvrRootComponent.setLicenses(imageIndexMainComponent.getLicenses());
        SbomUtils.setEvidenceIdentities(nvrRootComponent, evidencePurls, Field.PURL);

        generationToRepositories.put(generation.id(), repositories);

        return nvrRootComponent;
    }

    // Add a very long timeout because this method could potentially need to update hundreds of manifests
    protected Sbom saveReleaseManifestForGeneration(
            SbomGenerationRequest releaseGeneration,
            Bom productVersionBom,
            V1Beta1RequestRecord advisoryManifestsRecord,
            Map<String, List<RepositoryCoordinates>> generationToRepositories) {

        try {
            QuarkusTransaction.begin(QuarkusTransaction.beginOptions().timeout(INCREASED_TIMEOUT_SEC));

            // 1 - Save the release generation with the release manifest
            releaseGeneration = generationRequestRepository.findById(releaseGeneration.getId());
            releaseGeneration.setStatus(SbomGenerationStatus.FINISHED);

            // 1.1 - Create the Sbom entity
            Sbom sbom = Sbom.builder()
                    .withIdentifier(releaseGeneration.getIdentifier())
                    .withSbom(SbomUtils.toJsonNode(productVersionBom))
                    .withGenerationRequest(releaseGeneration)
                    .withConfigIndex(0)
                    .build();
            sbom = sbomService.save(sbom);

            // 2 - For every generation, find all the existing manifests and update the with release repo
            // data
            for (String generationId : generationToRepositories.keySet()) {

                // 2.1 - Select the repository with longest repoFragment + tag
                List<RepositoryCoordinates> repositories = generationToRepositories.get(generationId);
                RepositoryCoordinates preferredRepo = AdvisoryEventUtils.findPreferredRepo(repositories);

                // 2.2 - Regenerate the manifest purls using the preferredRepo and keep track of the updates, we need
                // them to update the index manifest variants
                Collection<V1Beta1RequestManifestRecord> buildManifests = advisoryManifestsRecord.manifests()
                        .stream()
                        .filter(manifest -> manifest.generation().id().equals(generationId))
                        .collect(Collectors.toList());
                Map<String, String> originalToRebuiltPurl = new HashMap<String, String>();
                buildManifests.stream().forEach(manifestRecord -> {
                    String rebuiltPurl = AdvisoryEventUtils.rebuildPurl(manifestRecord.rootPurl(), preferredRepo);
                    originalToRebuiltPurl.put(manifestRecord.rootPurl(), rebuiltPurl);
                });

                // 2.3 - For every manifest previously generated from this generation
                for (V1Beta1RequestManifestRecord buildManifestRecord : buildManifests) {

                    Sbom buildManifest = sbomService.get(buildManifestRecord.id());
                    Bom manifestBom = SbomUtils.fromJsonNode(buildManifest.getSbom());

                    // 2.4 Update rootPurl, metadata.component.purl, bom.component[0].purl with the rebuiltPurl
                    String rebuiltPurl = originalToRebuiltPurl.get(buildManifest.getRootPurl());
                    buildManifest.setRootPurl(rebuiltPurl);

                    if (manifestBom.getMetadata() != null && manifestBom.getMetadata().getComponent() != null) {
                        manifestBom.getMetadata().getComponent().setPurl(rebuiltPurl);
                        if (manifestBom.getMetadata().getComponent().getDescription() != null
                                && manifestBom.getMetadata()
                                        .getComponent()
                                        .getDescription()
                                        .contains(buildManifest.getRootPurl())) {

                            manifestBom.getMetadata()
                                    .getComponent()
                                    .getDescription()
                                    .replace(buildManifest.getRootPurl(), rebuiltPurl);
                        }
                    }
                    if (manifestBom.getComponents() != null && !manifestBom.getComponents().isEmpty()) {
                        manifestBom.getComponents().get(0).setPurl(rebuiltPurl);

                        // 2.5 - If there are variants (this is an index image) update also the purls with the rebuilt
                        // ones
                        if (manifestBom.getComponents().get(0).getPedigree() != null
                                && manifestBom.getComponents().get(0).getPedigree().getVariants() != null
                                && manifestBom.getComponents()
                                        .get(0)
                                        .getPedigree()
                                        .getVariants()
                                        .getComponents() != null) {

                            for (Component variant : manifestBom.getComponents()
                                    .get(0)
                                    .getPedigree()
                                    .getVariants()
                                    .getComponents()) {
                                if (originalToRebuiltPurl.containsKey(variant.getPurl())) {
                                    variant.setPurl(originalToRebuiltPurl.get(variant.getPurl()));
                                }
                            }
                        }

                        // 2.6 - Add an evidence.identity list with all the rebuilt purls
                        Set<String> evidencePurls = AdvisoryEventUtils
                                .rebuildPurls(buildManifest.getRootPurl(), repositories);
                        SbomUtils.setEvidenceIdentities(manifestBom.getComponents().get(0), evidencePurls, Field.PURL);
                    }

                    // 2.7 - Update the original Sbom
                    buildManifest.setSbom(SbomUtils.toJsonNode(manifestBom));

                    // TODO: should we add a "released" boolean to Sbom model?
                    // manifest.setReleased(true);
                }
            }

            QuarkusTransaction.commit();

            return sbom;
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

    private V1Beta1RequestManifestRecord findImageIndexManifest(
            V1Beta1RequestRecord advisoryManifestsRecord,
            V1Beta1GenerationRecord generation) {

        String imageIndexPurl = SbomUtils.createContainerImageOCIPurl(generation.identifier());
        if (imageIndexPurl == null) {
            throw new ApplicationException("Unable to compute PURL for generation '{}'", generation.identifier());
        }
        return advisoryManifestsRecord.manifests()
                .stream()
                .filter(
                        manifest -> manifest.generation().id().equals(generation.id())
                                && manifest.rootPurl().equals(imageIndexPurl))
                .findFirst()
                .orElseThrow(
                        () -> new ApplicationException(
                                "Image index manifest not found for generation '{}'",
                                generation.identifier()));
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
        metadata.setTimestamp(shipDate);
        metadata.setToolChoice(SbomUtils.createToolInformation(toolVersion));
        return metadata;
    }

    private String getGenerationNVRFromManifest(V1Beta1RequestManifestRecord manifestRecord) {
        GenerationRequestType generationRequestType = GenerationRequestType
                .fromName(manifestRecord.generation().type());

        if (GenerationRequestType.BREW_RPM.equals(generationRequestType)) {
            // The NVR is stored as the generation identifier
            return manifestRecord.generation().identifier();
        }

        if (GenerationRequestType.CONTAINERIMAGE.equals(generationRequestType)) {
            // The NVR is not stored inside the generation, we need to get it from the manifest. Might be optimized in
            // future.
            Sbom sbom = sbomService.get(manifestRecord.id());
            String[] nvr = SbomUtils.computeNVRFromContainerManifest(sbom.getSbom());
            if (nvr != null) {
                return String.join(NVR_STANDARD_SEPARATOR, nvr);
            }
        }

        // The are no NVRs associated with GenerationRequestType.BUILD or GenerationRequestType.OPERATION or
        // GenerationRequestType.ANALYSIS
        return null;
    }

    private Map<String, V1Beta1GenerationRecord> mapNVRToBuildGeneration(V1Beta1RequestRecord advisoryManifestsRecord) {
        Set<String> processedGenerationsIds = new HashSet<>();

        return advisoryManifestsRecord.manifests()
                .stream()
                .filter(manifest -> !processedGenerationsIds.contains(manifest.generation().id()))
                .map(manifest -> {
                    String nvr = getGenerationNVRFromManifest(manifest);
                    if (nvr != null) {
                        processedGenerationsIds.add(manifest.generation().id());
                    }
                    return Map.entry(nvr, manifest.generation());
                })
                .filter(entry -> entry.getKey() != null) // Filter out entries where NVR is null
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<ProductVersionEntry, Set<String>> mapProductVersionToCPEs(
            Map<ProductVersionEntry, List<BuildItem>> advisoryBuildDetails) {

        Map<ProductVersionEntry, Set<String>> productVersionToCPEs = new HashMap<>();
        advisoryBuildDetails.forEach((productVersionEntry, buildItems) -> {
            // Map all VariantArch to ErrataVariant and collect distinct ErrataVariant objects
            Set<String> productVersionCPEs = buildItems.stream()
                    .flatMap(buildItem -> buildItem.getVariantArch().keySet().stream())
                    .map(variantArch -> errataClient.getVariant(variantArch.toString()))
                    .filter(Objects::nonNull)
                    .map(errataVariant -> errataVariant.getData().getAttributes().getCpe())
                    .collect(Collectors.toSet());

            // Add more granular CPEs
            productVersionCPEs.addAll(AdvisoryEventUtils.createGranularCPEs(productVersionEntry, productVersionCPEs));

            productVersionToCPEs.put(productVersionEntry, productVersionCPEs);
        });
        return productVersionToCPEs;
    }

    private List<RepositoryCoordinates> getRepositoriesDetails(String nvr) {

        PyxisRepositoryDetails repositoriesDetails = pyxisClient.getRepositoriesDetails(nvr);
        return repositoriesDetails.getData()
                .stream()
                .flatMap(dataSection -> dataSection.getRepositories().stream())
                .filter(PyxisRepositoryDetails.Repository::isPublished)
                .filter(repository -> repository.getTags() != null)
                .flatMap(
                        repository -> repository.getTags()
                                .stream()
                                .filter(tag -> !"latest".equals(tag.getName()))
                                .map(
                                        tag -> new RepositoryCoordinates(
                                                repository.getRegistry(),
                                                repository.getRepository(),
                                                tag.getName())))
                .filter(repoCoordinate -> repoCoordinate.getRepositoryFragment() != null)
                .collect(Collectors.toList());
    }

    /*
     * Not used at the moment. Useful to understand if a repository has requires_terms = false meaning the repo is also
     * accessible without authentication
     */
    private PyxisRepository getRepository(String registry, String repository) {
        return pyxisClient.getRepository(registry, repository);
    }

}
