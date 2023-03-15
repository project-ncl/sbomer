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
import javax.inject.Inject;
import javax.persistence.NoResultException;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.NotFoundException;

import org.redhat.sbomer.model.ArtifactCache;
import org.redhat.sbomer.model.Sbom;
import org.redhat.sbomer.repositories.ArtifactCacheRepository;
import org.redhat.sbomer.repositories.SbomRepository;
import org.redhat.sbomer.service.generator.SBOMGenerator;
import org.redhat.sbomer.transformer.PncArtifactsToPropertiesSbomTransformer;
import org.redhat.sbomer.transformer.SbomManipulator;
import org.redhat.sbomer.utils.enums.GenerationMode;
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

    @Inject
    SBOMGenerator sbomGenerator;

    @Inject
    ArtifactInfoMapper artifactInfoMapper;

    @Inject
    Validator validator;

    @Inject
    SbomManipulator sbomManipulator;

    @Inject
    PncArtifactsToPropertiesSbomTransformer artifactsToPropertiesSbomTransformer;

    /**
     * Runs the generation of SBOM using the available implementation of the generator. This is done in an asynchronous
     * way -- the generation is run behind the scenes.
     *
     * @param buildId
     */
    public void createBomFromPncBuild(String buildId) {
        sbomGenerator.generate(buildId);
    }

    public Page<Sbom> listSboms(int pageIndex, int pageSize) {
        log.debug("Getting list of all base SBOMS with pageIndex: {}, pageSize: {}", pageIndex, pageSize);

        List<Sbom> collection = sbomRepository.findAll().page(pageIndex, pageSize).list();
        int totalPages = sbomRepository.findAll().page(io.quarkus.panache.common.Page.ofSize(pageSize)).pageCount();
        long totalHits = sbomRepository.findAll().count();
        List<Sbom> content = nullableStreamOf(collection).collect(Collectors.toList());

        return new Page<Sbom>(pageIndex, pageSize, totalPages, totalHits, content);
    }

    public Sbom getBaseSbom(String buildId) {
        log.debug("Getting base SBOMS with buildId: {}", buildId);
        try {
            return sbomRepository.getSbom(buildId, GenerationMode.BASE_CYCLONEDX);
        } catch (NoResultException nre) {
            throw new NotFoundException("Base SBOM for build id " + buildId + " not found.");
        }
    }

    public Sbom getSbom(String buildId, GenerationMode mode) {
        log.debug("Getting SBOM with buildId: {} and generationMode: {}", buildId, mode);
        try {
            return sbomRepository.getSbom(buildId, mode);
        } catch (NoResultException nre) {
            throw new NotFoundException("SBOM for build id " + buildId + " and mode " + mode + " not found.");
        }
    }

    /**
     * Persist changes to the {@link Sbom} in the database.
     *
     * @param sbom
     * @return
     */
    @Transactional
    public Sbom saveBaseSbom(Sbom sbom) throws ValidationException {
        log.debug("Storing base sbom: {}", sbom.toString());

        sbom.setGenerationTime(Instant.now());
        sbom.setId(Sequence.nextId());
        if (sbom.getGenerationMode() == null) {
            sbom.setGenerationMode(GenerationMode.BASE_CYCLONEDX);
        }

        Set<ConstraintViolation<Sbom>> violations = validator.validate(sbom);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }

        sbomRepository.persistAndFlush(sbom);
        return sbom;
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

    public Sbom runEnrichmentOfBaseSbom(String buildId, GenerationMode mode)
            throws NotFoundException, ValidationException {

        Sbom baseSBOM = getBaseSbom(buildId);
        Bom bom = baseSBOM.getCycloneDxBom();
        if (bom != null) {
            // TODO need to switch to async futures
            Bom enrichedBom = null;
            if (GenerationMode.ENRICHED_v1_0.equals(mode)) {
                enrichedBom = sbomManipulator.addTransformer(artifactsToPropertiesSbomTransformer).runTransformers(bom);

            } else {
                enrichedBom = sbomManipulator.addTransformer(artifactsToPropertiesSbomTransformer).runTransformers(bom);
            }

            BomJsonGenerator generator = BomGeneratorFactory.createJson(schemaVersion(), enrichedBom);

            // If there is already a SBOM enriched with this mode and for this buildId, we update it
            try {
                Sbom existingEnrichedSbom = sbomRepository.getSbom(buildId, mode);
                existingEnrichedSbom.setSbom(generator.toJsonNode());
                existingEnrichedSbom.setParentSbom(baseSBOM);
                return updateSbom(existingEnrichedSbom);
            } catch (NoResultException nre) {
                Sbom enrichedSbom = new Sbom();
                enrichedSbom.setBuildId(buildId);
                enrichedSbom.setGenerationMode(mode);
                enrichedSbom.setSbom(generator.toJsonNode());
                enrichedSbom.setParentSbom(baseSBOM);
                return saveSbom(enrichedSbom);
            }
        } else {
            throw new ValidationException("Could not convert initial SBOM of build " + buildId);
        }
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
