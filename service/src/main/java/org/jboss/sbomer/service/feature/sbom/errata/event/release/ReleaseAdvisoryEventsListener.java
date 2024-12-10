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
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1GenerationRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestManifestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata.Details;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.BuildItem;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.feature.sbom.errata.event.AdvisoryEventUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.pyxis.PyxisClient;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepository;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepositoryDetails;
import org.jboss.sbomer.service.feature.sbom.service.SbomGenerationRequestRepository;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.stats.StatsService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ReleaseAdvisoryEventsListener {

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

    private static final String NVR_UTILITY_SEPARATOR = "####";
    private static final String NVR_STANDARD_SEPARATOR = "-";

    public void onReleaseAdvisoryEvent(@ObservesAsync AdvisoryReleaseEvent event) {
        log.debug("Event received for advisory release ...");

        if (event.getErratum().getDetails().get().getContentTypes().contains("docker")) {
            releaseManifestsForDockerBuilds(event);
        } else {
            releaseManifestsForRPMBuilds();
        }
    }

    protected void releaseManifestsForRPMBuilds() {
        throw new ApplicationException("**** NOT IMPLEMENTED ****");
    }

    protected void releaseManifestsForDockerBuilds(AdvisoryReleaseEvent event) {

        Details details = event.getErratum().getDetails().get();
        V1Beta1RequestRecord advisoryManifestsRecord = event.getLatestAdvisoryManifestsRecord();
        Map<ProductVersionEntry, List<BuildItem>> advisoryBuildDetails = event.getAdvisoryBuildDetails();
        Map<ProductVersionEntry, SbomGenerationRequest> releaseGenerations = event.getReleaseGenerations();

        log.debug(
                "Creating release manifests for Docker builds of advisory: '{}'[{}]",
                details.getFulladvisory(),
                details.getId());

        String toolVersion = statsService.getStats().getVersion();
        Component.Type productType = AdvisoryEventUtils.getComponentTypeForProduct(details.getProduct().getShortName());

        // Create a map to associate each build (NVR) attached to an advisory to the generation it triggered
        Map<String, V1Beta1GenerationRecord> nvrToGeneration = mapNVRToGeneration(advisoryManifestsRecord);

        // Create a map to associate each ProductVersion to its list of CPEs
        Map<ProductVersionEntry, Set<String>> productVersionToCPEs = mapProductVersionToCPEs(advisoryBuildDetails);

        advisoryBuildDetails.forEach((productVersion, buildItems) -> {
            // Create the release manifest for this ProductVersion
            Set<String> cpes = productVersionToCPEs.get(productVersion);
            Bom productVersionBom = SbomUtils.createBom();
            Metadata productVersionMetadata = createMetadata(
                    productVersionBom,
                    productVersion.getDescription(),
                    productVersion.getName(),
                    productType,
                    cpes,
                    Date.from(details.getActualShipDate()),
                    toolVersion);
            Dependency productVersionDependency = new Dependency(productVersionMetadata.getComponent().getBomRef());

            // Add all the builds to the release manifest
            buildItems.forEach(
                    buildItem -> processBuildItem(
                            buildItem,
                            productVersionDependency,
                            advisoryManifestsRecord,
                            nvrToGeneration,
                            productVersionBom));

            productVersionBom.setDependencies(List.of(productVersionDependency));
            SbomUtils.addMissingSerialNumber(productVersionBom);

            SbomGenerationRequest releaseGeneration = releaseGenerations.get(productVersion);
            Sbom sbom = saveReleaseManifestForPendingGeneration(releaseGeneration, productVersionBom);
            log.info(
                    "Saved SBOM '{}' for generation '{}' for ProductVersion '{}' of errata '{}'",
                    sbom,
                    releaseGeneration.getId(),
                    productVersion.getName(),
                    details.getFulladvisory());
        });
    }

    protected void processBuildItem(
            BuildItem buildItem,
            Dependency releaseDependency,
            V1Beta1RequestRecord advisoryManifestsRecord,
            Map<String, V1Beta1GenerationRecord> nvrsToGenerations,
            Bom bom) {

        // From the generation triggered from this build (NVR), find the image index manifest and get the manifest
        // content, we need to copy the main component
        V1Beta1GenerationRecord generation = findGenerationForNVR(buildItem.getNvr(), nvrsToGenerations);
        String imageIndexPurl = SbomUtils.createContainerImageOCIPurl(generation.identifier());
        if (imageIndexPurl == null) {
            throw new ApplicationException("Unable to compute PURL for generation '{}'", generation.identifier());
        }
        V1Beta1RequestManifestRecord imageIndexManifest = findImageIndexManifest(
                advisoryManifestsRecord,
                generation,
                imageIndexPurl);
        Sbom imageIndexSbom = sbomService.get(imageIndexManifest.id());
        Component imageIndexMainComponent = SbomUtils.fromJsonNode(imageIndexSbom.getSbom()).getComponents().get(0);

        // Find where this build (NVR) has been published to
        List<PyxisRepositoryDetails.Repository> repositories = getRepositoriesDetails(buildItem.getNvr());

        // Create summary (pick the longest value) and evidence purl
        Set<String> summaryPurls = AdvisoryEventUtils
                .createPurls(repositories, imageIndexMainComponent.getVersion(), false);
        String imageIndexSelectedPurl = summaryPurls.iterator().next();
        Set<String> evidencePurls = AdvisoryEventUtils
                .createPurls(repositories, imageIndexMainComponent.getVersion(), true);

        // Finally create the root component for this build (NVR) from the image index manifest
        Component imageIndexRootComponent = SbomUtils.createComponent(
                null,
                imageIndexMainComponent.getName(),
                imageIndexMainComponent.getVersion(),
                null,
                imageIndexSelectedPurl,
                imageIndexMainComponent.getType());
        populateComponentMetadata(imageIndexRootComponent, imageIndexMainComponent, evidencePurls);

        // Add the component to the release manifest components and add the purl to the "provides" list
        bom.addComponent(imageIndexRootComponent);
        releaseDependency.addProvides(new Dependency(imageIndexSelectedPurl));

        // =========> TODO
        // Identify the location where the images were published (which one in case of multiple?) and update all the
        // manifests
    }

    private V1Beta1GenerationRecord findGenerationForNVR(
            String nvr,
            Map<String, V1Beta1GenerationRecord> nvrsToGenerations) {
        return nvrsToGenerations.entrySet()
                .stream()
                .filter(entry -> convertToStandardNVR(entry.getKey()).equals(nvr))
                .map(entry -> entry.getValue())
                .findFirst()
                .orElseThrow(() -> new ApplicationException("Generation not found for build '{}'", nvr));
    }

    private V1Beta1RequestManifestRecord findImageIndexManifest(
            V1Beta1RequestRecord advisoryManifestsRecord,
            V1Beta1GenerationRecord generation,
            String imageIndexPurl) {
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

    private void populateComponentMetadata(Component component, Component source, Set<String> evidencePurls) {
        component.setSupplier(source.getSupplier());
        component.setPublisher(source.getPublisher());
        component.setHashes(source.getHashes());
        component.setLicenses(source.getLicenses());
        SbomUtils.setEvidenceIdentities(component, evidencePurls, Field.PURL);
    }

    // ADD A VERY LONG TIMEOUT? THIS SHOULD BE DONE ATOMICALLY
    @Transactional()
    protected Sbom saveReleaseManifestForPendingGeneration(SbomGenerationRequest pendingGeneration, Bom bom) {

        pendingGeneration = generationRequestRepository.findById(pendingGeneration.getId());
        pendingGeneration.setStatus(SbomGenerationStatus.FINISHED);

        // Create the Sbom entity
        Sbom sbom = Sbom.builder()
                .withIdentifier(pendingGeneration.getIdentifier())
                .withSbom(SbomUtils.toJsonNode(bom))
                .withGenerationRequest(pendingGeneration)
                .withConfigIndex(0)
                .build();
        sbom = sbomService.save(sbom);

        return sbom;
    }

    private Metadata createMetadata(
            Bom bom,
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
        bom.setMetadata(metadata);
        return metadata;
    }

    private String convertToStandardNVR(String customNVR) {
        return customNVR.replace(NVR_UTILITY_SEPARATOR, NVR_STANDARD_SEPARATOR);
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
                return String.join(NVR_UTILITY_SEPARATOR, nvr);
            }
        }

        // The are no NVRs associated with GenerationRequestType.BUILD or GenerationRequestType.OPERATION or
        // GenerationRequestType.ANALYSIS
        return null;
    }

    private Map<String, V1Beta1GenerationRecord> mapNVRToGeneration(V1Beta1RequestRecord advisoryManifestsRecord) {
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

    private List<PyxisRepositoryDetails.Repository> getRepositoriesDetails(String nvr) {

        PyxisRepositoryDetails repositoriesDetails = pyxisClient.getRepositoriesDetails(nvr);
        return repositoriesDetails.getData()
                .stream()
                .flatMap(dataSection -> dataSection.getRepositories().stream())
                .filter(PyxisRepositoryDetails.Repository::isPublished)
                .peek(
                        repository -> repository.setTags(
                                repository.getTags()
                                        .stream()
                                        .filter(tag -> !"latest".equals(tag.getName())) // Exclude tags with name
                                                                                        // "latest"
                                        .collect(Collectors.toList())))
                .filter(repository -> repository.getTags() != null && !repository.getTags().isEmpty())
                .distinct()
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
