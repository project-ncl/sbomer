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

import java.util.List;
import java.util.Set;

import org.jboss.sbomer.core.dto.v1alpha2.SbomRecord;
import org.jboss.sbomer.core.errors.ValidationException;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.features.sbom.utils.UrlUtils;
import org.jboss.sbomer.service.feature.sbom.config.SbomerConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.NotificationService;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.rest.QueryParameters;
import org.jboss.sbomer.service.feature.sbom.rest.RestUtils;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;

/**
 * Main SBOM service that is dealing with the {@link Sbom} resource.
 */
@ApplicationScoped
@Slf4j
public class SbomService {

    @Inject
    SbomRepository sbomRepository;

    @Inject
    SbomGenerationRequestRepository sbomRequestRepository;

    @Inject
    Validator validator;

    @Inject
    NotificationService notificationService;

    @Inject
    SbomerConfig sbomerConfig;

    @WithSpan
    public long countSboms() {
        return sbomRepository.count();
    }

    @WithSpan
    public long countSbomGenerationRequests() {
        return sbomRequestRepository.count();
    }

    @WithSpan
    public long countInProgressSbomGenerationRequests() {
        return sbomRequestRepository
                .count("status != ?1 and status != ?2", SbomGenerationStatus.FINISHED, SbomGenerationStatus.FAILED);
    }

    @WithSpan
    public Page<SbomRecord> searchSbomRecordsByQueryPaginated(
            @SpanAttribute(value = "pageIndex") int pageIndex,
            @SpanAttribute(value = "pageSize") int pageSize,
            @SpanAttribute(value = "rsqlQuery") String rsqlQuery,
            @SpanAttribute(value = "sort") String sort) {

        QueryParameters parameters = QueryParameters.builder()
                .rsqlQuery(rsqlQuery)
                .sort(sort)
                .pageSize(pageSize)
                .pageIndex(pageIndex)
                .build();

        List<SbomRecord> content = sbomRepository.searchSbomRecords(parameters);
        Long count = sbomRepository.countByRsqlQuery(parameters.getRsqlQuery());

        return toPage(content, parameters, count);
    }

    @WithSpan
    public Page<Sbom> searchSbomsByQueryPaginated(
            @SpanAttribute(value = "pageIndex") int pageIndex,
            @SpanAttribute(value = "pageSize") int pageSize,
            @SpanAttribute(value = "rsqlQuery") String rsqlQuery,
            @SpanAttribute(value = "sort") String sort) {

        QueryParameters parameters = QueryParameters.builder()
                .rsqlQuery(rsqlQuery)
                .sort(sort)
                .pageSize(pageSize)
                .pageIndex(pageIndex)
                .build();

        List<Sbom> content = sbomRepository.search(parameters);
        Long count = sbomRepository.countByRsqlQuery(parameters.getRsqlQuery());

        return toPage(content, parameters, count);
    }

    @WithSpan
    public Page<SbomGenerationRequest> searchSbomRequestsByQueryPaginated(
            @SpanAttribute(value = "pageIndex") int pageIndex,
            @SpanAttribute(value = "pageSize") int pageSize,
            @SpanAttribute(value = "rsqlQuery") String rsqlQuery,
            @SpanAttribute(value = "sort") String sort) {

        QueryParameters parameters = QueryParameters.builder()
                .rsqlQuery(rsqlQuery)
                .sort(sort)
                .pageSize(pageSize)
                .pageIndex(pageIndex)
                .build();

        List<SbomGenerationRequest> content = sbomRequestRepository.search(parameters);
        Long count = sbomRequestRepository.countByRsqlQuery(parameters.getRsqlQuery());

        return toPage(content, parameters, count);
    }

    /**
     * Prepares a {@link Page} object with the result of the search.
     *
     * @param content The content to populate the page with.
     * @param parameters Query parameters passed to the search.
     * @return A {@link Page} element with content.
     */
    protected <X> Page<X> toPage(List<X> content, QueryParameters parameters, Long count) {
        int totalPages = 0;

        if (count == 0) {
            totalPages = 1; // a single page of zero results
        } else {
            totalPages = (int) Math.ceil((double) count / (double) parameters.getPageSize());
        }

        return new Page<>(parameters.getPageIndex(), parameters.getPageSize(), totalPages, count, content);
    }

    /**
     * Returns {@link Sbom} for the specified identifier.
     *
     * @param sbomId As {@link String}.
     * @return The {@link Sbom} object.
     */
    @WithSpan
    public Sbom get(@SpanAttribute(value = "sbomId") String sbomId) {
        return sbomRepository.findById(sbomId);
    }

    /**
     * Delete the SBOM Generation Request and all its associated SBOMs from the database.
     *
     * @param id The {@link SbomGenerationRequest} id to delete from the database.
     */
    @WithSpan
    @Transactional
    public void deleteSbomRequest(@SpanAttribute(value = "id") String id) {
        log.info("Deleting SBOM Generation Request: {}", id);

        sbomRequestRepository.deleteRequest(id);
    }

    /**
     * Persists given {@link Sbom} in the database.
     *
     * @param sbom The {@link Sbom} resource to store in database.
     * @return Updated {@link Sbom} resource.
     */
    @WithSpan
    @Transactional
    public Sbom save(Sbom sbom) {
        log.debug("Preparing for storing SBOM: {}", sbom.toString());

        sbom.setId(RandomStringIdGenerator.generate());

        validate(sbom);

        log.debug("Storing SBOM in the database: {}", sbom.toString());

        sbom = sbomRepository.saveSbom(sbom);

        log.debug("SBOM id: '{}' stored!", sbom.getId());

        return sbom;
    }

    /**
     * Validates given {@link Sbom}.
     *
     * @param sbom
     */
    private void validate(Sbom sbom) {
        log.debug("Performing validation of SBOM: {}", sbom);

        Set<ConstraintViolation<Sbom>> violations = validator.validate(sbom);

        if (!violations.isEmpty()) {
            throw new ValidationException(
                    "SBOM validation error",
                    RestUtils.constraintViolationsToMessages(violations));
        }

        log.debug("SBOM '{}' is valid!", sbom.getId());
    }

    /**
     * Searches for the latest generated SBOM matching the provided {@code purl}.
     *
     * @param purl
     * @return The latest generated SBOM or {@code null}.
     */
    public Sbom findByPurl(String purl) {
        String polishedPurl = UrlUtils.removeAllowedQualifiersFromPurl(purl, sbomerConfig.purlQualifiersAllowList());
        log.debug("Trying to find latest generated SBOM for purl: '{}' (polished to '{}')", purl, polishedPurl);

        QueryParameters parameters = QueryParameters.builder()
                .rsqlQuery("rootPurl=eq='" + polishedPurl + "'")
                .sort("creationTime=desc=")
                .pageSize(10)
                .pageIndex(0)
                .build();

        List<Sbom> sboms = sbomRepository.search(parameters);

        log.debug("Found {} results for the '{}' purl", sboms.size(), polishedPurl);

        if (sboms.isEmpty()) {
            return null;
        }

        return sboms.get(0);

    }

    /**
     * Notify the creation of an existing SBOM via UMB.
     *
     * @param sbom The {@link Sbom} resource to nofify about.
     */
    @WithSpan
    public void notifyCompleted(@SpanAttribute(value = "sbom") Sbom sbom) {
        log.info("Notifying the generation of SBOM: {}", sbom);
        notificationService.notifyCompleted(List.of(sbom));
    }
}
