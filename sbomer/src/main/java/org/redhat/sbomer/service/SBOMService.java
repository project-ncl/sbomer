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
import org.redhat.sbomer.model.BaseSBOM;
import org.redhat.sbomer.repositories.ArtifactCacheRepository;
import org.redhat.sbomer.repositories.BaseSBOMRepository;
import org.redhat.sbomer.service.generator.SBOMGenerator;
import org.redhat.sbomer.validation.exceptions.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;

import org.cyclonedx.model.Bom;
import org.cyclonedx.BomGeneratorFactory;
import org.cyclonedx.CycloneDxSchema.Version;
import org.cyclonedx.generators.json.BomJsonGenerator;
import org.jboss.pnc.common.concurrent.Sequence;
import org.jboss.pnc.common.json.JsonUtils;
import org.jboss.pnc.dto.Artifact;
import org.redhat.sbomer.dto.ArtifactInfo;
import org.redhat.sbomer.dto.response.Page;
import org.redhat.sbomer.mappers.api.ArtifactCacheMapper;
import org.redhat.sbomer.mappers.api.ArtifactInfoMapper;
import org.redhat.sbomer.mappers.api.BaseSBOMMapper;

import lombok.extern.slf4j.Slf4j;

import static org.redhat.sbomer.utils.SbomUtils.schemaVersion;

/**
 * Main SBOM service that is dealing with the {@link BaseSBOM} resource.
 */
@ApplicationScoped
@Slf4j
public class SBOMService {

    @Inject
    BaseSBOMRepository baseSbomRepository;

    @Inject
    ArtifactCacheRepository artifactCacheRepository;

    @Inject
    PNCService pncService;

    @Inject
    SBOMGenerator sbomGenerator;

    @Inject
    BaseSBOMMapper baseSBOMMapper;

    @Inject
    ArtifactCacheMapper artifactCacheMapper;

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
    public void createBomFromPncBuild(String buildId) {
        sbomGenerator.generate(buildId);
    }

    public Page<org.redhat.sbomer.dto.BaseSBOM> listBaseSboms(int pageIndex, int pageSize) {
        log.debug("Getting list of all base SBOMS with pageIndex: {}, pageSize: {}", pageIndex, pageSize);

        List<BaseSBOM> collection = baseSbomRepository.findAll().page(pageIndex, pageSize).list();
        int totalPages = baseSbomRepository.findAll().page(io.quarkus.panache.common.Page.ofSize(pageSize)).pageCount();
        long totalHits = baseSbomRepository.findAll().count();
        List<org.redhat.sbomer.dto.BaseSBOM> content = nullableStreamOf(collection).map(baseSBOMMapper::toDTO)
                .collect(Collectors.toList());

        return new Page<org.redhat.sbomer.dto.BaseSBOM>(pageIndex, pageSize, totalPages, totalHits, content);
    }

    public org.redhat.sbomer.dto.BaseSBOM getBaseSbom(String buildId) {
        log.debug("Getting base SBOMS with buildId: {}", buildId);
        try {
            BaseSBOM dbEntity = baseSbomRepository.getBaseSbom(buildId);
            return baseSBOMMapper.toDTO(dbEntity);
        } catch (NoResultException nre) {
            throw new NotFoundException("Base SBOM for build id " + buildId + " not found.");
        }
    }

    /**
     * Persist changes to the {@link BaseSBOM} in the database.
     *
     * @param baseSbom
     * @return
     */
    @Transactional
    public org.redhat.sbomer.dto.BaseSBOM saveBom(org.redhat.sbomer.dto.BaseSBOM baseSbom) throws ValidationException {
        log.debug("Storing entity: " + baseSbom.toString());
        BaseSBOM dbEntity = baseSBOMMapper.toEntity(baseSbom);

        Set<ConstraintViolation<BaseSBOM>> violations = validator.validate(dbEntity);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }

        dbEntity.setGenerationTime(Instant.now());
        dbEntity.setId(Sequence.nextId());
        baseSbomRepository.persistAndFlush(dbEntity);
        return baseSBOMMapper.toDTO(dbEntity);
    }

    @Transactional
    public org.redhat.sbomer.dto.BaseSBOM updateBom(String id, Bom bom) throws ValidationException {
        log.info("Updating SBOM of existing baseSBOM with id: {}", id);

        BaseSBOM dbEntity = baseSbomRepository.findById(Long.valueOf(id));
        BomJsonGenerator generator = BomGeneratorFactory.createJson(schemaVersion(), bom);
        dbEntity.setSbom(generator.toJsonNode());

        Set<ConstraintViolation<BaseSBOM>> violations = validator.validate(dbEntity);
        if (!violations.isEmpty()) {
            log.info(
                    "violations: {}",
                    violations.stream().map(e -> e.getMessage().toString()).collect(Collectors.joining("\n\t")));
            throw new ValidationException(violations);
        }

        baseSbomRepository.getEntityManager().merge(dbEntity);
        return baseSBOMMapper.toDTO(dbEntity);
    }

    public Page<org.redhat.sbomer.dto.ArtifactCache> listArtifactCache(int pageIndex, int pageSize) {
        log.debug("Getting list of all base artifact caches with pageIndex: {}, pageSize: {}", pageIndex, pageSize);

        List<ArtifactCache> collection = artifactCacheRepository.findAll().page(pageIndex, pageSize).list();
        int totalPages = artifactCacheRepository.findAll()
                .page(io.quarkus.panache.common.Page.ofSize(pageSize))
                .pageCount();
        long totalHits = artifactCacheRepository.findAll().count();
        List<org.redhat.sbomer.dto.ArtifactCache> content = nullableStreamOf(collection).map(artifactCacheMapper::toDTO)
                .collect(Collectors.toList());

        return new Page<org.redhat.sbomer.dto.ArtifactCache>(pageIndex, pageSize, totalPages, totalHits, content);
    }

    public org.redhat.sbomer.dto.ArtifactCache getArtifactCache(String purl) {
        log.debug("Getting artifact properties with purl: {}", purl);
        try {
            ArtifactCache dbEntity = artifactCacheRepository.getArtifactCache(purl);
            return artifactCacheMapper.toDTO(dbEntity);
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
    public org.redhat.sbomer.dto.ArtifactCache saveArtifactCache(org.redhat.sbomer.dto.ArtifactCache artifactCache)
            throws ValidationException {
        log.debug("Storing entity: " + artifactCache.toString());
        ArtifactCache dbEntity = artifactCacheMapper.toEntity(artifactCache);

        Set<ConstraintViolation<ArtifactCache>> violations = validator.validate(dbEntity);
        if (!violations.isEmpty()) {
            throw new ValidationException(violations);
        }

        dbEntity.setId(Sequence.nextId());
        artifactCacheRepository.persistAndFlush(dbEntity);
        return artifactCacheMapper.toDTO(dbEntity);
    }

    @Transactional
    public org.redhat.sbomer.dto.ArtifactCache fetchArtifact(String purl) {

        try {
            org.redhat.sbomer.dto.ArtifactCache cachedArtifact = getArtifactCache(purl);
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
            org.redhat.sbomer.dto.ArtifactCache artifactCache = org.redhat.sbomer.dto.ArtifactCache.builder()
                    .purl(purl)
                    .info(node)
                    .build();

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
