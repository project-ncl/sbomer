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
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ValidationException;
import org.jboss.sbomer.generator.Generator.GeneratorLiteral;
import org.jboss.sbomer.generator.SbomGenerator;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.processor.Processor.ProcessorLiteral;
import org.jboss.sbomer.processor.SbomProcessor;
import org.jboss.sbomer.rest.RestUtils;
import org.jboss.sbomer.rest.dto.Page;

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
     * Runs the generation of SBOM using the available implementation of the generator. This is done in an asynchronous
     * way -- the generation is run behind the scenes.
     *
     * @param buildId
     */
    public void generateSbomFromPncBuild(String buildId, GeneratorImplementation generator) {
        generators.select(GeneratorLiteral.of(generator)).get().generate(buildId);
    }

    /**
     * Performs processing of the provided SBOM using selected processor.
     *
     * @param sbom {@link Sbom} object to process
     * @param processor Selected {@link ProcessorImplementation}
     */
    public void processSbom(Sbom sbom, ProcessorImplementation processor) {
        processSbom(sbom.getId(), processor);
    }

    /**
     * Performs processing of the SBOM identified by the ID using selected processor.
     *
     * @param sbomId SBOM identifier being a {@link Long}
     * @param processor Selected {@link ProcessorImplementation}
     */
    public void processSbom(Long sbomId, ProcessorImplementation processor) {
        processors.select(ProcessorLiteral.of(processor)).get().process(sbomId);
    }

    /**
     * Get list of {@link Sbom}s in a paginated way.
     */
    public Page<Sbom> listSboms(int pageIndex, int pageSize) {
        log.debug("Getting list of all base SBOMS with pageIndex: {}, pageSize: {}", pageIndex, pageSize);

        List<Sbom> collection = sbomRepository.findAll().page(pageIndex, pageSize).list();
        int totalPages = sbomRepository.findAll().page(io.quarkus.panache.common.Page.ofSize(pageSize)).pageCount();
        long totalHits = sbomRepository.findAll().count();
        List<Sbom> content = nullableStreamOf(collection).collect(Collectors.toList());

        return new Page<Sbom>(pageIndex, pageSize, totalPages, totalHits, content);
    }

    /**
     * Get list of {@link Sbom}s for a given PNC build ID in a paginated way.
     */
    public Page<Sbom> listAllSbomsWithBuildId(String buildId, int pageIndex, int pageSize) {
        log.debug("Getting list of all SBOMS with buildId: {}", buildId);

        List<Sbom> collection = sbomRepository.getAllSbomWithBuildIdQuery(buildId).page(pageIndex, pageSize).list();
        int totalPages = sbomRepository.getAllSbomWithBuildIdQuery(buildId)
                .page(io.quarkus.panache.common.Page.ofSize(pageSize))
                .pageCount();
        long totalHits = sbomRepository.getAllSbomWithBuildIdQuery(buildId).count();
        List<Sbom> content = nullableStreamOf(collection).collect(Collectors.toList());

        return new Page<Sbom>(pageIndex, pageSize, totalPages, totalHits, content);
    }

    /**
     * Get list of {@link Sbom}s for a given PNC build ID in a paginated way.
     */
    public Sbom getSbom(String buildId, GeneratorImplementation generator, ProcessorImplementation processor) {
        log.info("Getting SBOM with buildId: {} and generator: {} and processor: {}", buildId, generator, processor);
        try {
            return sbomRepository.getSbom(buildId, generator, processor);
        } catch (NoResultException nre) {
            throw new NotFoundException(
                    "SBOM for build id " + buildId + " and generator " + generator + " and processor " + processor
                            + " not found.");
        }
    }

    /**
     * Returns {@link Sbom} for the specified identifier.
     *
     * @param sbomId
     * @return The {@link Sbom} object.
     */
    public Sbom getSbomById(long sbomId) {
        return sbomRepository.findById(sbomId);
    }

    /**
     * Persist changes to the {@link Sbom} in the database.
     *
     * @param sbom
     * @return
     */
    @Transactional
    public Sbom saveSbom(Sbom sbom) throws ApplicationException {
        log.debug("Storing sbom: {}", sbom.toString());

        sbom.setGenerationTime(Instant.now());
        sbom.setId(Sequence.nextId());

        Set<ConstraintViolation<Sbom>> violations = validator.validate(sbom);

        if (!violations.isEmpty()) {
            throw new ValidationException(
                    "SBOM validation error",
                    RestUtils.constraintViolationsToMessages(violations));
        }

        sbomRepository.persistAndFlush(sbom);

        // In case of a base SBOM, we start the default processing automatically
        if (sbom.getProcessor() == null) {
            processSbom(sbom, ProcessorImplementation.DEFAULT);
        }

        return sbom;
    }

    public static <T> Stream<T> nullableStreamOf(Collection<T> nullableCollection) {
        if (nullableCollection == null) {
            return Stream.empty();
        }
        return nullableCollection.stream();
    }
}