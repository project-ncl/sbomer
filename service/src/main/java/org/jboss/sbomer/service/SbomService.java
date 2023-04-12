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
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.NotFoundException;

import org.jboss.pnc.common.concurrent.Sequence;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.enums.SbomStatus;
import org.jboss.sbomer.core.enums.SbomType;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ValidationException;
import org.jboss.sbomer.generator.Generator.GeneratorLiteral;
import org.jboss.sbomer.generator.SbomGenerator;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.processor.Processor.ProcessorLiteral;
import org.jboss.sbomer.processor.SbomProcessor;
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

    @Any
    @Inject
    Instance<SbomGenerator> generators;

    @Any
    @Inject
    Instance<SbomProcessor> processors;

    @Inject
    Validator validator;

    /**
     * Runs the generation of SBOM using the available implementation of the generator.
     *
     * Generation is done in an asynchronous way. The returned object is a handle which will let the client to retrieve
     * the SBOM content once the generation is finished.
     *
     * @param buildId The PNC build ID for which the generation should be performed.
     * @param generator The selected generator implementation, see {@link GeneratorImplementation}
     * @return Returns a {@link Sbom} object that will represent the generated CycloneDX sbom.
     */
    public Sbom generate(String buildId, GeneratorImplementation generator) {

        try {
            // Return in case we have it generated already
            return sbomRepository.getSbom(buildId, generator, null);
        } catch (NoResultException ex) {
            // Ignored
        }

        Sbom sbom = new Sbom();
        sbom.setStatus(SbomStatus.GENERATING);
        sbom.setType(SbomType.BUILD_TIME); // TODO Is it always the case?
        sbom.setBuildId(buildId);
        sbom.setGenerator(generator);

        // Store it in database
        sbom = save(sbom);

        // Schedule the generation
        generators.select(GeneratorLiteral.of(generator)).get().generate(sbom.getId());

        return sbom;
    }

    /**
     * Performs processing of the SBOM identified by the ID using selected processor.
     *
     * @param sbomId SBOM identifier being a {@link Long}
     * @param processor Selected {@link ProcessorImplementation}
     */
    public Sbom process(Sbom sbom, ProcessorImplementation processor) {
        log.debug("Preparing to process SBOM id '{}' with '{}' processor", sbom.getId(), processor);

        // Create the child object
        Sbom child = sbom.giveBirth();

        // Set the correct status
        child.setStatus(SbomStatus.PROCESSING);

        // Store the child in database
        child = save(child);

        // Schedule processing
        processors.select(ProcessorLiteral.of(processor)).get().process(sbom.getId());

        return child;
    }

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

    // /**
    // * Get list of {@link Sbom}s for a given PNC build ID in a paginated way.
    // */
    // public Sbom get(String buildId, GeneratorImplementation generator, ProcessorImplementation processor) {
    // log.info("Getting SBOM with buildId: {} and generator: {} and processor: {}", buildId, generator, processor);
    // try {
    // return sbomRepository.getSbom(buildId, generator, processor);
    // } catch (NoResultException nre) {
    // throw new NotFoundException(
    // "SBOM for build id " + buildId + " and generator " + generator + " and processor " + processor
    // + " not found.");
    // }
    // }

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
     * @param sbomId
     * @return The {@link Sbom} object.
     */
    public Sbom get(Long sbomId) {
        return sbomRepository.findById(sbomId);
    }

    /**
     * Persists changes to given {@link Sbom} in the database.
     *
     * The difference between the {@link SbomService#create(Sbom)} method is that this one is used for updating
     * already-existing resources in the database.
     *
     * @param sbom The {@link Sbom} resource to store in database.
     * @return Updated {@link Sbom} resource.
     */
    @Transactional
    public Sbom updateBom(Long sbomId, JsonNode bom) {
        Sbom sbom = sbomRepository.findById(sbomId);

        if (sbom == null) {
            throw new ApplicationException("Could not find SBOM with ID '{}'", sbomId);
        }

        // Update the SBOM field
        sbom.setSbom(bom);
        // and status
        sbom.setStatus(SbomStatus.READY);

        log.debug("Updating SBOM: {}", sbom.toString());

        validate(sbom);
        sbom = sbomRepository.getEntityManager().merge(sbom);

        log.debug("SBOM '{}' updated!", sbom.getId());

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

        // Sbom stored = sbomRepository.findById(sbom.getId());

        log.debug("Storing sbom: {}", sbom.toString());

        sbom.setGenerationTime(Instant.now());
        sbom.setId(Sequence.nextId());

        validate(sbom);

        sbomRepository.persistAndFlush(sbom);

        // TODO
        // In case of a base SBOM, we start the default processing automatically
        // if (sbom.getProcessor() == null && sbom.getSbom() != null) {
        // process(sbom, ProcessorImplementation.DEFAULT);
        // }

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
