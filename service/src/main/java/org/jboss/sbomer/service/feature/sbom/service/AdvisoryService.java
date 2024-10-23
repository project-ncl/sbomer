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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.build.finder.koji.ClientSession;
import org.jboss.sbomer.core.features.sbom.config.BrewRPMConfig;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata.Details;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataProduct;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.Build;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.BuildItem;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;

import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    SbomService sbomService;

    public Collection<SbomGenerationRequest> generateFromAdvisory(String advisoryId) {

        // Fetching Erratum
        Errata erratum = errataClient.getErratum(advisoryId);
        if (erratum == null || erratum.getDetails().isEmpty()) {
            log.warn("Could not retrieve the erratum details for id : '{}'", advisoryId);
            return Collections.emptyList();
        }

        // Will be removed, leave now for debugging
        printAllErratumData(erratum);

        if (!erratum.getDetails().get().getTextonly()) {
            return handleStandardAdvisory(erratum);
        } else {
            return handleTextOnlyAdvisory(erratum);
        }
    }

    private Collection<SbomGenerationRequest> handleTextOnlyAdvisory(Errata erratum) {
        // Handle the text-only advisories
        log.warn("** TODO ** Handle the Text-only advisories");
        return Collections.emptyList();
    }

    private Collection<SbomGenerationRequest> handleStandardAdvisory(Errata erratum) {
        log.info(
                "Advisory {} ({}) is standard (non Text-Only), with status {}",
                erratum.getDetails().get().getFulladvisory(),
                erratum.getDetails().get().getId(),
                erratum.getDetails().get().getStatus());

        Details details = erratum.getDetails().get();
        if (details.getContentTypes().isEmpty() || details.getContentTypes().size() > 1) {
            log.warn(
                    "** ??? ** Zero or multiple content-types ({}), will need to handle this!",
                    details.getContentTypes());
            return Collections.emptyList();
        }

        if (ErrataStatus.QE.equals(details.getStatus())) {
            return handleStandardQEAdvisory(details);
        } else {
            log.warn("** TODO ** Handle the SHIPPED-LIVE advisories");
            return Collections.emptyList();
        }
    }

    private Collection<SbomGenerationRequest> handleStandardQEAdvisory(Details details) {
        log.debug("Handle standard QE Advisory {}", details);

        if (details.getContentTypes().stream().noneMatch(type -> type.equals("docker") || type.equals("rpm"))) {
            log.warn("** TODO ** Unknown content-type :{}", details.getContentTypes());
            return Collections.emptyList();
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

        if (details.getContentTypes().contains("docker")) {
            return processDockerBuilds(buildDetails);
        } else {
            ErrataProduct product = errataClient.getProduct(details.getProduct().getShortName());
            return processRPMBuilds(details, product, buildDetails);
        }
    }

    private Collection<SbomGenerationRequest> processDockerBuilds(
            Map<ProductVersionEntry, List<BuildItem>> buildDetails) {
        log.debug("Processing docker builds: {}", buildDetails);

        Collection<SbomGenerationRequest> sbomRequests = new ArrayList<SbomGenerationRequest>();
        SyftImageConfig config = SyftImageConfig.builder().withIncludeRpms(true).build();

        buildDetails.forEach((pVersion, items) -> {
            log.debug("Processing container builds of Errata Product Version: '{}'", pVersion);

            items.forEach(item -> {
                log.debug("Getting image information from Brew for build '{}' ({})", item.getNvr(), item.getId());

                String imageName = getImageNameFromBuild(item.getId());
                if (imageName != null) {
                    log.debug("Creating GenerationRequest Kubernetes resource...");
                    sbomRequests.add(sbomService.generateSyftImage(imageName, config));
                }
            });
        });
        return sbomRequests;
    }

    private Collection<SbomGenerationRequest> processRPMBuilds(
            Details details,
            ErrataProduct product,
            Map<ProductVersionEntry, List<BuildItem>> buildDetails) {
        log.debug("Processing RPM builds: {}", buildDetails);

        Collection<SbomGenerationRequest> sbomRequests = new ArrayList<SbomGenerationRequest>();

        buildDetails.forEach((pVersion, items) -> {
            log.debug(
                    "Processing RPM builds of Errata Product '{}' with Product Version: '{}'",
                    product.getData().getAttributes().getName(),
                    pVersion);

            if (items == null || items.isEmpty()) {
                log.warn(
                        "There are no RPM builds associated with Errata {} and Product Version: '{}'",
                        details.getId(),
                        pVersion);
                return;
            }

            // Find the Product Version id from the ErrataProduct metadata
            Long pVersionId = findErrataProductVersionIdByName(product, pVersion.getName());

            List<String> brewBuildIds = items.stream()
                    .map(item -> item.getId().toString())
                    .collect(Collectors.toList());
            BrewRPMConfig config = BrewRPMConfig.builder()
                    .withAdvisoryId(String.valueOf(details.getId()))
                    .withProductVersionId(pVersionId != null ? String.valueOf(pVersionId) : pVersion.getName())
                    .withBrewBuildIds(brewBuildIds)
                    .build();

            log.debug("Creating GenerationRequest Kubernetes resource...");

            GenerationRequest req = new GenerationRequestBuilder(GenerationRequestType.BREW_RPM)
                    .withIdentifier(config.getAdvisoryId() + "-" + config.getProductVersionId())
                    .withStatus(SbomGenerationStatus.NEW)
                    .withConfig(config)
                    .build();

            log.debug("ConfigMap to create: '{}'", req);

            SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.sync(req);
            kubernetesClient.configMaps().resource(req).create();

            sbomRequests.add(sbomGenerationRequest);
        });

        return sbomRequests;
    }

    public Long findErrataProductVersionIdByName(ErrataProduct product, String pVersionName) {
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

    private String getImageNameFromBuild(Long buildId) {
        try {
            KojiBuildInfo buildInfo = kojiSession.getBuild(buildId.intValue());
            if (buildInfo == null) {
                return null;
            }

            // Get the image name from the extra.image.index.pull
            // Retrieve the image name with the sha256 if available, otherwise the first in the list
            return Optional.ofNullable(buildInfo.getExtra())
                    .map(extra -> (Map<String, Object>) extra.get("image"))
                    .map(imageMap -> (Map<String, Object>) imageMap.get("index"))
                    .map(indexMap -> (List<String>) indexMap.get("pull"))
                    .flatMap(
                            list -> list != null && !list.isEmpty()
                                    ? list.stream().filter(item -> item.contains("sha256")).findFirst()
                                    : Optional.empty())
                    .or(
                            () -> Optional.ofNullable(buildInfo.getExtra())
                                    .map(extra -> (Map<String, Object>) extra.get("image"))
                                    .map(imageMap -> (Map<String, Object>) imageMap.get("index"))
                                    .map(indexMap -> (List<String>) indexMap.get("pull"))
                                    .flatMap(
                                            list -> list != null && !list.isEmpty() ? list.stream().findFirst()
                                                    : Optional.empty()))
                    .orElse(null);
        } catch (KojiClientException e) {
            log.error("Unable to fetch containers information for buildID: {}", buildId, e);
            return null;
        }
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
