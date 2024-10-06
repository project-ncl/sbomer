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

import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataMessageHelper;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.ErrataStatusChangeMessageBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
@Setter
public class ErrataNotificationHandler {

    @Inject
    @RestClient
    ErrataClient errataClient;

    public void handle(String message) throws JsonProcessingException {

        ErrataStatusChangeMessageBody errataStatusChange = ErrataMessageHelper.fromStatusChangeMessage(message);
        log.info("Fetching Errata information for erratum with id {}", errataStatusChange.getErrataId());

        Errata erratum = errataClient.getErratum(String.valueOf(errataStatusChange.getErrataId()));
        log.info("Fetched erratum \n{}", erratum);

        if (erratum.getDetails().isEmpty()) {
            log.info("Mmmmm I don't know how to get the release information, group_id is empty...");
            return;
        }

        ErrataRelease erratumRelease = errataClient
                .getReleases(String.valueOf(erratum.getDetails().get().getGroupId()));
        log.info("Fetched erratum release {}", erratumRelease);
        log.info(
                "** This advisory is related to Product '{}' and ProductRelease '{}'. We don't know how to retrieve the ProductVariant yet.**",
                erratumRelease.getData().getRelationships().getProduct().getShortName(),
                erratumRelease.getData().getAttributes().getName());

        Optional<JsonNode> notes = erratum.getNotesMapping();
        if (notes.isEmpty()) {
            log.info("The erratum does not contain any JSON content inside the notes...");
        }
        log.info("The erratum contains a notes content with following JSON: \n{}", notes.get().toPrettyString());

    }

}
