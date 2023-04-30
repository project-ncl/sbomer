/**
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
package org.jboss.sbomer.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.jboss.pnc.common.concurrent.Sequence;
import org.jboss.sbomer.config.ProcessingConfig;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.errors.ValidationException;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.rest.RestUtils;
import org.jboss.sbomer.rest.dto.Page;

import com.fasterxml.jackson.databind.JsonNode;

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
    ProcessingConfig processingConfig;

    @Inject
    Validator validator;

    /**
     * Get list of {@link Sbom}s in a paginated way.
     */
    // TODO: Should we return Page here?
    public Page<Sbom> list(int pageIndex, int pageSize) {
        log.debug("Getting list of all base SBOMS with pageIndex: {}, pageSize: {}", pageIndex, pageSize);

        List<Sbom> collection = sbomRepository.findAll().page(pageIndex, pageSize).list();
        int totalPages = sbomRepository.findAll().page(io.quarkus.panache.common.Page.ofSize(pageSize)).pageCount();
        long totalHits = sbomRepository.findAll().count();
        List<Sbom> content = Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .collect(Collectors.toList());

        return new Page<Sbom>(pageIndex, pageSize, totalPages, totalHits, content);
    }

    /**
     * Get list of {@link Sbom}s for a given PNC build ID.
     */
    public List<Sbom> listAllSbomsWithBuildId(String buildId) {
        log.debug("Getting list of all SBOMS with buildId: {}", buildId);

        List<Sbom> collection = sbomRepository.getAllSbomWithBuildIdQuery(buildId).list();
        return Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .collect(Collectors.toList());
    }

    /**
     * Get list of {@link Sbom}s for a given PNC build ID in a paginated way.
     */
    // TODO: Should we return Page here?
    public Page<Sbom> list(String buildId, int pageIndex, int pageSize) {
        log.debug("Getting list of all SBOMS with buildId: {}", buildId);

        List<Sbom> collection = sbomRepository.getAllSbomWithBuildIdQuery(buildId).page(pageIndex, pageSize).list();
        int totalPages = sbomRepository.getAllSbomWithBuildIdQuery(buildId)
                .page(io.quarkus.panache.common.Page.ofSize(pageSize))
                .pageCount();
        long totalHits = sbomRepository.getAllSbomWithBuildIdQuery(buildId).count();
        List<Sbom> content = Optional.ofNullable(collection)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .collect(Collectors.toList());

        return new Page<Sbom>(pageIndex, pageSize, totalPages, totalHits, content);
    }

    /**
     * Get base {@link Sbom} for a given PNC build ID.
     */
    public Sbom getBaseSbomByBuildId(String buildId) {
        log.info("Getting base SBOM with buildId: {}", buildId);
        try {
            return sbomRepository.getBaseSbomByBuildId(buildId);
        } catch (NoResultException nre) {
            throw new NotFoundException("Base SBOM for build id " + buildId + " not found.");
        }
    }

    /**
     * Get enriched {@link Sbom} for a given PNC build ID.
     */
    public Sbom getEnrichedSbomByBuildId(String buildId) {
        log.info("Getting enriched SBOM with buildId: {}", buildId);
        try {
            return sbomRepository.getEnrichedSbomByBuildId(buildId);
        } catch (NoResultException nre) {
            throw new NotFoundException("Enriched SBOM for build id " + buildId + " not found.");
        }
    }

    /**
     * Get base {@link Sbom} for a given root purl.
     */
    public Sbom getBaseSbomByRootPurl(String rootPurl) {
        log.info("Getting base SBOM with root purl: {}", rootPurl);
        try {
            return sbomRepository.getBaseSbomByRootPurl(rootPurl);
        } catch (NoResultException nre) {
            throw new NotFoundException("Base SBOM for root purl " + rootPurl + " not found.");
        }
    }

    /**
     * Get enriched {@link Sbom} for a given root purl.
     */
    public Sbom getEnrichedSbomByRootPurl(String rootPurl) {
        log.info("Getting enriched SBOM with root purl: {}", rootPurl);
        try {
            return sbomRepository.getEnrichedSbomByRootPurl(rootPurl);
        } catch (NoResultException nre) {
            throw new NotFoundException("Enriched SBOM for root purl " + rootPurl + " not found.");
        }
    }

    /**
     * Returns {@link Sbom} for the specified identifier.
     *
     * @param sbomId As {@link String}.
     * @return The {@link Sbom} object.
     */
    public Sbom get(String sbomId) {
        try {
            return get(Long.valueOf(sbomId));
        } catch (NumberFormatException e) {
            throw new ClientException("Invalid SBOM id provided: '{}', a number was expected", sbomId);
        }
    }

    /**
     * Returns {@link Sbom} for the specified identifier.
     *
     * @param sbomId
     * @return The {@link Sbom} object.
     */
    public Sbom get(Long sbomId) {
        return sbomRepository.findById(sbomId);
    }

    /**
     * Persists changes to given {@link Sbom} in the database.
     *
     * The difference between the {@link SbomService#save(Sbom)} method is that this one is used for updating
     * already-existing resources in the database.
     *
     * @param sbom The {@link Sbom} resource to store in database.
     * @return Updated {@link Sbom} resource.
     */
    @Transactional
    public Sbom updateBom(Long sbomId, JsonNode bom) {
        Sbom sbom = sbomRepository.findById(sbomId);

        if (sbom == null) {
            throw new NotFoundException("Could not find SBOM with ID '{}'", sbomId);
        }

        // Update the SBOM field
        sbom.setSbom(bom);

        log.debug("Updating SBOM: {}", sbom.toString());

        validate(sbom);

        sbom = sbomRepository.saveSbom(sbom);

        log.debug("SBOM '{}' updated!", sbomId);

        return sbom;
    }

    /**
     * Persists given {@link Sbom} in the database.
     *
     * @param sbom The {@link Sbom} resource to store in database.
     * @return Updated {@link Sbom} resource.
     */
    @Transactional
    public Sbom save(Sbom sbom) {
        log.debug("Preparing for storing SBOM: {}", sbom.toString());

        sbom.setGenerationTime(Instant.now());
        sbom.setId(Sequence.nextId());

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
}
