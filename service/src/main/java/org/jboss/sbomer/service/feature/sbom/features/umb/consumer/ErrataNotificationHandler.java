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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.build.finder.koji.ClientSession;
import org.jboss.sbomer.core.features.sbom.config.SyftImageConfig;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataMessageHelper;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.Build;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.BuildItem;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata.Details;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.ErrataStatusChangeMessageBody;
import org.jboss.sbomer.service.feature.sbom.model.UMBMessage;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class ErrataNotificationHandler {

    @Inject
    @RestClient
    @Setter
    ErrataClient errataClient;

    @Inject
    @Setter
    FeatureFlags featureFlags;

    @Inject
    @Setter
    ClientSession kojiSession;

    @Inject
    @Setter
    SbomService sbomService;

    public void handle(UMBMessage message) throws JsonProcessingException, IOException {
        if (!featureFlags.errataIntegrationEnabled()) {
            log.warn("Errata API integration is disabled, the UMB message won't be used!!");
            return;
        }

        ErrataStatusChangeMessageBody errataStatusChange = ErrataMessageHelper
                .fromStatusChangeMessage(message.getContent());
        log.info("Fetching Errata information for erratum with id {}...", errataStatusChange.getErrataId());

        if (!isRelevantStatus(errataStatusChange.getStatus())) {
            log.warn("Received a status change that is not QE nor SHIPPED_LIVE, ignoring it");
            return;
        }

        // Fetching Erratum
        Errata erratum = errataClient.getErratum(String.valueOf(errataStatusChange.getErrataId()));
        if (erratum == null || erratum.getDetails().isEmpty()) {
            log.warn("Could not retrieve the erratum details for id : '{}'", errataStatusChange.getErrataId());
            return;
        }

        // Will be removed, leave now for debugging
        String summary = retrieveAllErratumData(erratum);
        System.out.println(summary);

        if (!erratum.getDetails().get().getTextonly()) {
            handleStandardAdvisory(erratum);
        } else {
            handleTextOnlyAdvisory(erratum);
        }
    }

    private boolean isRelevantStatus(ErrataStatus status) {
        return status == QE || status == SHIPPED_LIVE;
    }

    private void handleTextOnlyAdvisory(Errata erratum) {
        // Handle the text-only advisories
        log.warn("** TODO ** Handle the Text-only advisories");
    }

    private void handleStandardAdvisory(Errata erratum) {
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
            return;
        }

        if (ErrataStatus.QE.equals(details.getStatus())) {
            handleStandardQEAdvisory(details);
        } else {

        }
    }

    private void handleStandardQEAdvisory(Details details) {
        log.debug("Handle standard QE Advisory {}", details);
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
            processDockerBuilds(buildDetails);
        } else if (details.getContentTypes().contains("rpm")) {
            log.warn("** TODO ** Handle RPM content-type");
        } else {
            log.warn("** TODO ** Unknown content-type :{}", details.getContentTypes());
        }
    }

    private void processDockerBuilds(Map<ProductVersionEntry, List<BuildItem>> buildDetails) {
        log.debug("Processing docker builds: {}", buildDetails);
        SyftImageConfig config = SyftImageConfig.builder().withIncludeRpms(true).build();
        buildDetails.forEach((pVersion, items) -> {
            log.debug("Processing container builds of Errata Product Version: '{}'", pVersion);
            items.forEach(item -> {
                log.debug("Getting image information from Brew for build '{}' ({})", item.getNvr(), item.getId());
                String imageName = getImageNameFromBuild(item.getId());
                if (imageName != null) {
                    sbomService.generateSyftImage(imageName, config);
                }
            });
        });
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

    private String retrieveAllErratumData(Errata erratum) {

        Optional<JsonNode> notes = erratum.getNotesMapping();

        if (notes.isEmpty()) {
            log.info("The erratum does not contain any JSON content inside the notes...");
        } else {
            log.info("The erratum contains a notes content with following JSON: \n{}", notes.get().toPrettyString());
        }

        if (erratum.getDetails().isEmpty()) {
            log.warn("Mmmmm I don't know how to get the release information...");
            return "";
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
        return summary.toString();
    }

}
