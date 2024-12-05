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
import org.cyclonedx.model.Hash;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.component.evidence.Identity.Field;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1GenerationRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestManifestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.errata.PyxisClient;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.PyxisRepository;
import org.jboss.sbomer.service.feature.sbom.errata.dto.PyxisRepositoryDetails;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata.Details;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.BuildItem;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.feature.sbom.errata.event.AdvisoryEventUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.stats.StatsService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ReleaseAdvisoryEventsListener {

    @Inject
    @RestClient
    ErrataClient errataClient;

    @Inject
    @RestClient
    PyxisClient pyxisClient;

    @Inject
    SbomService sbomService;

    @Inject
    StatsService statsService;

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

    private void releaseManifestsForRPMBuilds() {
        throw new ApplicationException("**** NOT IMPLEMENTED ****");
    }

    private void releaseManifestsForDockerBuilds(AdvisoryReleaseEvent event) {

        Details details = event.getErratum().getDetails().get();
        V1Beta1RequestRecord advisoryManifestsRecord = event.getLatestAdvisoryManifestsRecord();
        Map<ProductVersionEntry, List<BuildItem>> advisoryBuildDetails = event.getAdvisoryBuildDetails();
        Map<ProductVersionEntry, SbomGenerationRequest> pendingGenerations = event.getPendingGenerations();

        log.debug(
                "Creating release manifests for Docker builds of advisory: '{}'[{}]",
                details.getFulladvisory(),
                details.getId());

        String toolVersion = statsService.getStats().getVersion();
        Component.Type productType = AdvisoryEventUtils.getComponentTypeForProduct(details.getProduct().getShortName());

        // Create a map to associate each distinct generation to its NVR
        Map<String, V1Beta1GenerationRecord> nvrToSuccessfulGenerations = getNVRToSuccessfulGenerations(
                advisoryManifestsRecord);

        // Create a map to associate each ProductVersion to its list of CPEs
        Map<ProductVersionEntry, Set<String>> productVersionToCPEs = getProductVersionToCPEs(advisoryBuildDetails);

        for (ProductVersionEntry productVersionEntry : advisoryBuildDetails.keySet()) {
            List<BuildItem> buildItems = advisoryBuildDetails.get(productVersionEntry);
            SbomGenerationRequest pendingGeneration = pendingGenerations.get(productVersionEntry);
            Set<String> cpes = productVersionToCPEs.get(productVersionEntry);
            String productName = productVersionEntry.getDescription();
            String productVersion = productVersionEntry.getName();

            Bom bom = SbomUtils.createBom();
            Metadata metadata = new Metadata();

            Component mainComponent = SbomUtils
                    .createComponent(null, productName, productVersion, null, null, productType);
            SbomUtils.setSupplier(mainComponent);
            SbomUtils.setEvidenceIdentities(mainComponent, cpes, Field.CPE);

            metadata.setComponent(mainComponent);
            metadata.setTimestamp(Date.from(details.getActualShipDate()));
            metadata.setToolChoice(SbomUtils.createToolInformation(toolVersion));
            bom.setMetadata(metadata);

            for (BuildItem buildItem : buildItems) {
                // Map each buildItem of this ProductVersion with a past successful generation, via NVR.
                Map.Entry<String, V1Beta1GenerationRecord> nvrGeneration = nvrToSuccessfulGenerations.entrySet()
                        .stream()
                        .filter(entry -> convertToStandardNVR(entry.getKey()).equals(buildItem.getNvr()))
                        .findFirst()
                        .orElse(null);

                if (nvrGeneration == null) {
                    continue;
                }

                Component.Type type = AdvisoryEventUtils.getComponentTypeForGeneration(nvrGeneration.getValue());
                String[] nvrTokenized = nvrGeneration.getKey().split(NVR_UTILITY_SEPARATOR);
                String name = nvrTokenized[0];
                String version = nvrTokenized[1] + "-" + nvrTokenized[2];
                Hash hash = AdvisoryEventUtils.retrieveHashFromGeneration(nvrGeneration.getValue());

                List<PyxisRepositoryDetails.Repository> repositories = getRepositoriesDetails(
                        convertToStandardNVR(nvrGeneration.getKey()));

                // Create summary and fully-qualified purls
                Set<String> summaryPurls = AdvisoryEventUtils.createPurls(repositories, hash, true);
                Set<String> evidencePurls = AdvisoryEventUtils.createPurls(repositories, hash, false);

                Component generationComponent = SbomUtils
                        .createComponent(null, name, version, null, summaryPurls.iterator().next(), type);
                SbomUtils.setSupplier(generationComponent);
                SbomUtils.addHashIfMissing(generationComponent, hash.getValue(), hash.getAlgorithm());
                SbomUtils.setEvidenceIdentities(generationComponent, evidencePurls, Field.PURL);

                bom.addComponent(generationComponent);
            }

            Sbom sbom = saveReleaseManifestForPendingGeneration(pendingGeneration, bom);
            log.info(
                    "Saved sbom {} for generation {} for ProductVersion {} of errata '{}'[{}]",
                    sbom.getId(),
                    pendingGeneration.getId(),
                    productVersionEntry.getName(),
                    details.getFulladvisory(),
                    details.getId());
        }
    }

    @Transactional
    protected Sbom saveReleaseManifestForPendingGeneration(SbomGenerationRequest pendingGeneration, Bom bom) {

        AdvisoryEventUtils.addMissingSerialNumber(bom);

        pendingGeneration = SbomGenerationRequest.findById(pendingGeneration.getId());
        pendingGeneration.setStatus(SbomGenerationStatus.FINISHED);

        // Create the Sbom entity
        Sbom sbom = Sbom.builder()
                .withIdentifier(pendingGeneration.getIdentifier())
                .withSbom(SbomUtils.toJsonNode(bom))
                .withGenerationRequest(pendingGeneration)
                .withConfigIndex(0)
                .build();
        return sbomService.save(sbom);
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
            return String.join(NVR_UTILITY_SEPARATOR, SbomUtils.computeNVRFromContainerManifest(sbom.getSbom()));
        }

        // The are no NVRs associated with GenerationRequestType.BUILD or GenerationRequestType.OPERATION or
        // GenerationRequestType.ANALYSIS
        return null;
    }

    private Map<String, V1Beta1GenerationRecord> getNVRToSuccessfulGenerations(
            V1Beta1RequestRecord advisoryManifestsRecord) {

        Set<String> processedGenerationsIds = new HashSet<String>();
        return advisoryManifestsRecord.manifests()
                .stream()
                .filter(manifest -> processedGenerationsIds.add(manifest.generation().id()))
                .map(manifest -> Map.entry(getGenerationNVRFromManifest(manifest), manifest.generation()))
                .filter(entry -> entry.getKey() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<ProductVersionEntry, Set<String>> getProductVersionToCPEs(
            Map<ProductVersionEntry, List<BuildItem>> advisoryBuildDetails) {

        Map<ProductVersionEntry, Set<String>> productVersionToCPEs = new HashMap<>();
        advisoryBuildDetails.forEach((productVersionEntry, buildItems) -> {
            // Map all VariantArch to ErrataVariant and collect distinct ErrataVariant objects
            Set<String> productVersionCPEs = buildItems.stream()
                    .flatMap(buildItem -> buildItem.getVariantArch().values().stream())
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
