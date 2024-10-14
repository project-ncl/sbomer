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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataMessageHelper;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.Build;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataVariant;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease.ErrataProductVersion;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.ErrataStatusChangeMessageBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

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

    public void handle(String message) throws JsonProcessingException {

        ErrataStatusChangeMessageBody errataStatusChange = ErrataMessageHelper.fromStatusChangeMessage(message);
        log.info("Fetching Errata information for erratum with id {}...", errataStatusChange.getErrataId());

        if (!featureFlags.errataIntegrationEnabled()) {
            log.warn("Errata API integration is disabled, the UMB message won't be used!!");
            return;
        }

        String summary = retrieveAllErratumData(errataStatusChange.getErrataId());
        System.out.println(summary);
    }

    private String retrieveAllErratumData(Long errataId) {

        Errata erratum = errataClient.getErratum(String.valueOf(errataId));

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
        ErrataBuildList erratumBuildList = errataClient.getBuildsList(String.valueOf(errataId));

        String summary = "\n**********************************\n";
        summary += "ID: " + erratum.getDetails().get().getId();
        summary += "\nTYPE: " + erratum.getOriginalType();
        summary += "\nAdvisory: " + erratum.getDetails().get().getFulladvisory();
        summary += "\nSynopsis: " + erratum.getDetails().get().getSynopsis();
        summary += "\nStatus: " + erratum.getDetails().get().getStatus();
        summary += "\nCVE: " + erratum.getContent().getContent().getCve();
        summary += "\n\nProduct: " + erratum.getDetails().get().getProduct().getName() + "("
                + erratum.getDetails().get().getProduct().getShortName() + ")";
        summary += "\nRelease: " + erratumRelease.getData().getAttributes().getName();
        summary += "\nProduct Versions: ";

        for (ErrataProductVersion productVersion : erratumRelease.getData().getRelationships().getProductVersions()) {
            log.info("Fetching Erratum product version variant for product version {}...", productVersion.getId());
            summary += "\n\tName: " + productVersion.getName();
            Collection<ErrataVariant.VariantData> errataVariants = errataClient.getVariantOfProductAndProductVersion(
                    erratum.getDetails().get().getProduct().getShortName(),
                    productVersion.getId());
            for (ErrataVariant.VariantData variant : errataVariants) {
                summary += "\n\tVariant: " + variant.getAttributes().getName() + " (CPE: "
                        + variant.getAttributes().getCpe() + ")";
            }
        }
        summary += "\n\nBuilds: ";
        if (erratumBuildList != null) {
            if (erratumBuildList.getProductVersions() != null && erratumBuildList.getProductVersions().size() > 0) {
                List<Build> builds = erratumBuildList.getProductVersions()
                        .entrySet()
                        .iterator()
                        .next()
                        .getValue()
                        .getBuilds();
                for (Build build : builds) {
                    summary += build.getBuildItems().values().stream().map(buildItem -> {
                        return buildItem.getId() + " (" + buildItem.getNvr() + ")";
                    }).collect(Collectors.joining(", "));
                }
            }
        }
        if (notes.isPresent()) {
            summary += "\nJSON Notes:\n" + notes.get().toPrettyString();
        } else {
            if (erratum.getContent().getContent().getNotes() != null
                    && erratum.getContent().getContent().getNotes().trim().length() > 0) {
                summary += "\nNotes:\n" + erratum.getContent().getContent().getNotes();
            }
        }

        summary += "\n**********************************\n";
        return summary;
    }

}
