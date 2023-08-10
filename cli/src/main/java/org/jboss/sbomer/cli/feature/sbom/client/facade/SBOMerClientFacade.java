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
package org.jboss.sbomer.cli.feature.sbom.client.facade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.rest.api.parameters.PaginationParameters;
import org.jboss.sbomer.cli.feature.sbom.client.SBOMerClient;
import org.jboss.sbomer.cli.feature.sbom.model.Sbom;
import org.jboss.sbomer.cli.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.oracle.svm.core.annotate.Inject;

import lombok.extern.slf4j.Slf4j;

/**
 * A service to interact with the SBOMer system via the SBOMerClient.
 */
@Slf4j
@ApplicationScoped
public class SBOMerClientFacade {

    @Inject
    @RestClient
    private SBOMerClient sbomerClient;

    public List<SbomGenerationRequest> searchSuccessfulGenerations(String buildId) {
        PaginationParameters pagParams = new PaginationParameters();
        pagParams.setPageIndex(0);
        pagParams.setPageSize(20);

        String rsqlQuery = "buildId=eq=" + buildId + ";status=eq=FINISHED;result=eq=SUCCESS";
        String sortQuery = "creationTime=desc=";

        log.info(
                "Searching existing successful SBOM Generation Requests with rsqlQuery: {}, sortQuery: {}",
                rsqlQuery,
                sortQuery);

        Response response = sbomerClient.searchGenerationRequests(buildId, pagParams, rsqlQuery, sortQuery);

        String json = response.readEntity(String.class);
        TypeReference<Page<SbomGenerationRequest>> typeReference = new TypeReference<Page<SbomGenerationRequest>>() {
        };
        try {
            Page<SbomGenerationRequest> sbomRequests = ObjectMapperProvider.json().readValue(json, typeReference);
            if (sbomRequests.getTotalHits() > 0) {
                return new ArrayList<>(sbomRequests.getContent());
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not find existing successful SBOM Generation Requests for PNC build '{}'", buildId, e);
        }
        return Collections.emptyList();
    }

    public SbomGenerationRequest searchLastSuccessfulGeneration(String buildId) {

        List<SbomGenerationRequest> sbomRequests = searchSuccessfulGenerations(buildId);
        Optional<SbomGenerationRequest> latestSbomRequest = Stream.ofNullable(sbomRequests)
                .flatMap(Collection::stream)
                .max(Comparator.comparing(SbomGenerationRequest::getCreationTime));
        return latestSbomRequest.orElse(null);
    }

    public List<Sbom> searchSbomsOfRequest(String requestId) {
        PaginationParameters pagParams = new PaginationParameters();
        pagParams.setPageIndex(0);
        pagParams.setPageSize(20);

        String rsqlQuery = "generationRequest.id=eq=" + requestId;
        String rsqlSort = null;

        log.info("Searching existing successful SBOM Generation Requests with rsqlQuery: {}", rsqlQuery);

        Response response = sbomerClient.searchSboms(requestId, pagParams, rsqlQuery, rsqlSort);

        String json = response.readEntity(String.class);
        TypeReference<Page<Sbom>> typeReference = new TypeReference<Page<Sbom>>() {
        };
        try {
            Page<Sbom> sboms = ObjectMapperProvider.json().readValue(json, typeReference);
            if (sboms.getTotalHits() > 0) {
                return new ArrayList<>(sboms.getContent());
            }
        } catch (JsonProcessingException e) {
            log.warn("Could not find SBOMs for Generation Request '{}'", requestId, e);
        }
        return Collections.emptyList();
    }

    public Sbom searchSbomsOfRequest(String requestId, int productIndex) {
        List<Sbom> sboms = searchSbomsOfRequest(requestId);
        Optional<Sbom> matchingSbom = Stream.ofNullable(sboms)
                .flatMap(Collection::stream)
                .filter(s -> s.getConfigIndex() == productIndex)
                .findFirst();
        return matchingSbom.orElse(null);
    }

}