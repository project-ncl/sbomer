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
package org.jboss.sbomer.service.nextgen.generator.rhrelease;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationResult;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.generator.AbstractGenerator;
import org.jboss.sbomer.service.nextgen.core.rest.SBOMerClient;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class RedHatReleaseGenerator extends AbstractGenerator {

    private RedHatReleaseGenerator() {
        super(null, null);
    }

    @Inject
    public RedHatReleaseGenerator(@RestClient SBOMerClient sbomerClient, ManagedExecutor managedExecutor) {
        super(sbomerClient, managedExecutor);
    }

    @Override
    public Set<String> getSupportedTypes() {
        return Set.of("EVENT");
    }

    @Override
    public void generate(GenerationRecord generationRecord) {
        log.info("Requested Red Hat release manifests as part of generation '{}'", generationRecord.id());

        log.debug("Reading generation request...");
        GenerationRequest request = JacksonUtils.parse(GenerationRequest.class, generationRecord.request());

        EventRecord eventRecord;

        log.debug("Fetching event with identifier '{}'...", request.target().identifier());

        try {
            eventRecord = sbomerClient.getEvent(request.target().identifier());
        } catch (NotFoundException ex) {
            throw new ApplicationException(
                    "Event with id '{}' could not be found, cannot process generation",
                    request.target().identifier(),
                    ex);
        }

        List<JsonNode> boms = new ArrayList<>();

        // Iterate over each generation that was part of that particular event and create copies
        // updates
        for (GenerationRecord g : eventRecord.generations()) {
            log.info("Processing generation '{}'", g.id());

            for (ManifestRecord m : g.manifests()) {
                log.info("Processing manifest '{}'", m.id());

                JsonNode bom = sbomerClient.getManifestContent(m.id());

                boms.add(bom);
            }
        }

        // Apply qualifier transformations to all manifests
        boms.forEach(m -> adjustQualifiers(m));

        // Create release manifest
        boms.add(createReleaseManifest(boms));

        if (boms.isEmpty()) {
            log.info("No manifests to upload...");

            updateStatus(
                    generationRecord.id(),
                    GenerationStatus.FAILED,
                    GenerationResult.ERR_GENERAL,
                    "No manifests were generated");
        }

        // Save all manifests linked to the current generation
        upload(boms, generationRecord.id());
    }

    @Retry(maxRetries = 5, delay = 10, delayUnit = ChronoUnit.SECONDS, abortOn = NotFoundException.class)
    protected void upload(List<JsonNode> boms, String generationId) {
        log.info("Uploading {} manifests...", boms.size());

        for (JsonNode bom : boms) {
            log.info("Uploading manifest...");
            ManifestRecord manifestRecord = sbomerClient.uploadManifest(generationId, bom);
            log.info("Manifest uploaded, registered with id '{}", manifestRecord.id());
        }

        updateStatus(
                generationId,
                GenerationStatus.FINISHED,
                GenerationResult.SUCCESS,
                "Release manifest generation completed successfully");
    }

    private void adjustQualifiers(JsonNode bom) {
        log.info("Adjusting qualifiers...");

        log.info("Qualifiers adjusted");
    }

    private JsonNode createReleaseManifest(List<JsonNode> boms) {
        log.info("Creating release manifest...");

        log.info("Release manifest created");

        return JsonNodeFactory.instance.objectNode();
    }
}
