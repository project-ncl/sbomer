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
package org.redhat.sbomer.service;

import java.io.IOException;
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
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.redhat.sbomer.model.ArtifactCache;
import org.redhat.sbomer.model.Sbom;
import org.redhat.sbomer.processor.SbomProcessor;
import org.redhat.sbomer.repositories.ArtifactCacheRepository;
import org.redhat.sbomer.repositories.SbomRepository;
import org.redhat.sbomer.utils.enums.Generators;
import org.redhat.sbomer.utils.enums.Processors;
import org.redhat.sbomer.validation.exceptions.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;

import org.cyclonedx.model.Bom;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.jboss.pnc.common.concurrent.Sequence;
import org.jboss.pnc.common.json.JsonUtils;
import org.jboss.pnc.dto.Artifact;
import org.redhat.sbomer.dto.ArtifactInfo;
import org.redhat.sbomer.dto.response.Page;
import org.redhat.sbomer.generator.SbomGenerator;
import org.redhat.sbomer.mappers.api.ArtifactInfoMapper;

import lombok.extern.slf4j.Slf4j;

import static org.redhat.sbomer.utils.SbomUtils.schemaVersion;

/**
 * Main SBOM service that is dealing with the {@link Sbom} resource.
 */
@ApplicationScoped
@Slf4j
public class SBOMService {

    @Inject
    SbomRepository sbomRepository;

    @Inject
    ArtifactCacheRepository artifactCacheRepository;

    @Inject
    PNCService pncService;

    @Any
    @Inject
    Instance<SbomGenerator> generators;

    @Any
    @Inject
    Instance<SbomProcessor> processors;

    @Inject
    ArtifactInfoMapper artifactInfoMapper;

    @Inject
    Validator validator;

    /**
     * Runs the generation of SBOM using the available implementation of the generator. This is done in an asynchronous
     * way -- the generation is run behind the scenes.
     *
     * @param buildId
     */
    public Response generateSbomFromPncBuild(String buildId, Generators generator, Processors processor) {

        generators.select(generator.getSelector()).get().generate(buildId, processor);
        return Response.status(Status.ACCEPTED).build();
    }

    public Page<Sbom> listSboms(int pageIndex, int pageSize) {
        log.debug("Getting list of all base SBOMS with pageIndex: {}, pageSize: {}", pageIndex, pageSize);

        List<Sbom> collection = sbomRepository.findAll().page(pageIndex, pageSize).list();
        int totalPages = sbomRepository.findAll().page(io.quarkus.panache.common.Page.ofSize(pageSize)).pageCount();
        long totalHits = sbomRepository.findAll().count();
        List<Sbom> content = nullableStreamOf(collection).collect(Collectors.toList());

        return new Page<Sbom>(pageIndex, pageSize, totalPages, totalHits, content);
    }

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

    public Sbom getSbom(String buildId, Generators generator, Processors processor) {
        log.debug("Getting SBOM with buildId: {} and generator: {} and processor: {}", buildId, generator, processor);
        try {
            return sbomRepository.getSbom(buildId, generator, processor);
        } catch (NoResultException nre) {
            throw new NotFoundException(
                    "SBOM for build id " + buildId + " and generator " + generator + " and processor " + processor
                            + " not found.");
        }
    }

    /**
     * Persist changes to the {@link Sbom} in the database.
     *
     * @param sbom
     * @return
     */
    @Transactional
    public Sbom saveSbom(Sbom sbom) throws ValidationException {
        log.debug("Storing sbom: {}", sbom.toString());

        sbom.setGenerationTime(Instant.now());
        sbom.setId(Sequence.nextId());

        Set<ConstraintViolation<Sbom>> violations = validator.validate(sbom);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }

        sbomRepository.persistAndFlush(sbom);
        return sbom;
    }

    @Transactional
    public Sbom updateSbom(Sbom sbom) throws ValidationException {
        log.debug("Updating sbom: {}", sbom.toString());

        sbom.setGenerationTime(Instant.now());

        Set<ConstraintViolation<Sbom>> violations = validator.validate(sbom);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }

        sbomRepository.getEntityManager().merge(sbom);
        return sbom;
    }

    @Transactional
    public Sbom updateBom(Long id, Bom bom) throws ValidationException {
        log.info("Updating SBOM of existing baseSBOM with id: {}", id);

        Sbom dbEntity = sbomRepository.findById(id);
        BomJsonGenerator generator = BomGeneratorFactory.createJson(schemaVersion(), bom);
        dbEntity.setSbom(generator.toJsonNode());

        Set<ConstraintViolation<Sbom>> violations = validator.validate(dbEntity);
        if (!violations.isEmpty()) {
            log.info(
                    "violations: {}",
                    violations.stream().map(e -> e.getMessage().toString()).collect(Collectors.joining("\n\t")));
            throw new ValidationException(violations);
        }

        sbomRepository.getEntityManager().merge(dbEntity);
        return dbEntity;
    }

    private Sbom runEnrichmentOfBaseSbom(Sbom baseSbom, Processors processor) {

        Bom bom = baseSbom.getCycloneDxBom();
        if (bom != null) {
            Bom enrichedBom = processors.select(processor.getSelector()).get().process(bom);
            BomJsonGenerator bomGenerator = BomGeneratorFactory.createJson(schemaVersion(), enrichedBom);
            // If there is already a SBOM enriched with this mode and for this buildId, we update it try { Sbom
            try {
                Sbom existingEnrichedSbom = getSbom(baseSbom.getBuildId(), baseSbom.getGenerator(), processor);
                existingEnrichedSbom.setSbom(bomGenerator.toJsonNode());
                existingEnrichedSbom.setParentSbom(baseSbom);
                return updateSbom(existingEnrichedSbom);
            } catch (NotFoundException nre) {
                Sbom enrichedSbom = new Sbom();
                enrichedSbom.setBuildId(baseSbom.getBuildId());
                enrichedSbom.setGenerator(baseSbom.getGenerator());
                enrichedSbom.setProcessor(processor);
                enrichedSbom.setSbom(bomGenerator.toJsonNode());
                enrichedSbom.setParentSbom(baseSbom);
                return saveSbom(enrichedSbom);
            }
        }
        throw new ValidationException("Could not convert initial SBOM of build " + baseSbom.getBuildId());
    }

    public Sbom saveAndEnrichSbom(Sbom sbom, Processors processor) throws ValidationException {
        log.debug(
                "Saving sbom for buildId {} and generator: {}, and running enrichment with processor: {}",
                sbom.getBuildId(),
                sbom.getGenerator(),
                processor);

        Sbom baseSbom = saveSbom(sbom);
        return runEnrichmentOfBaseSbom(baseSbom, processor);
    }

    public Page<ArtifactCache> listArtifactCache(int pageIndex, int pageSize) {
        log.debug("Getting list of all base artifact caches with pageIndex: {}, pageSize: {}", pageIndex, pageSize);

        List<ArtifactCache> collection = artifactCacheRepository.findAll().page(pageIndex, pageSize).list();
        int totalPages = artifactCacheRepository.findAll()
                .page(io.quarkus.panache.common.Page.ofSize(pageSize))
                .pageCount();
        long totalHits = artifactCacheRepository.findAll().count();
        List<ArtifactCache> content = nullableStreamOf(collection).collect(Collectors.toList());

        return new Page<ArtifactCache>(pageIndex, pageSize, totalPages, totalHits, content);
    }

    public ArtifactCache getArtifactCache(String purl) {
        log.debug("Getting artifact properties with purl: {}", purl);
        try {
            return artifactCacheRepository.getArtifactCache(purl);
        } catch (NoResultException nre) {
            throw new NotFoundException("Artifact info for purl " + purl + " not found.");
        }
    }

    /**
     * Persist changes to the {@link ArtifactCache} in the database.
     *
     * @param baseSbom
     * @return
     */
    @Transactional
    public ArtifactCache saveArtifactCache(ArtifactCache artifactCache) throws ValidationException {
        log.debug("Storing entity: " + artifactCache.toString());

        Set<ConstraintViolation<ArtifactCache>> violations = validator.validate(artifactCache);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }

        artifactCache.setId(Sequence.nextId());
        artifactCacheRepository.persistAndFlush(artifactCache);
        return artifactCache;
    }

    @Transactional
    public ArtifactCache fetchArtifact(String purl) {

        try {
            ArtifactCache cachedArtifact = getArtifactCache(purl);
            log.info("Artifact with purl {} found in cache!", purl);
            return cachedArtifact;
        } catch (NotFoundException nre) {
            log.info("Artifact with purl {} not found in cache, will fetch from PNC", purl);
        }

        Artifact artifact = pncService.getArtifact(purl);
        if (artifact == null) {
            throw new NotFoundException("Artifact with purl " + purl + " not found in PNC.");
        }

        try {
            ArtifactInfo info = artifactInfoMapper.toArtifactInfo(artifact);
            info.setBuildSystem("PNC");
            String jsonString = JsonUtils.toJson(info);
            JsonNode node = JsonUtils.fromJson(jsonString, JsonNode.class);
            ArtifactCache artifactCache = new ArtifactCache();
            artifactCache.setInfo(node);
            artifactCache.setPurl(purl);
            return saveArtifactCache(artifactCache);
        } catch (IOException ioExc) {
            throw new ValidationException(
                    "Could not convert artifatc with purl " + purl + ", exception msg: " + ioExc.getMessage());
        }
    }

    public static <T> Stream<T> nullableStreamOf(Collection<T> nullableCollection) {
        if (nullableCollection == null) {
            return Stream.empty();
        }
        return nullableCollection.stream();
    }
}
