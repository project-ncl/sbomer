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
package org.jboss.sbomer.feature.sbom.core.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.jboss.pnc.common.concurrent.Sequence;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.errors.ValidationException;
import org.jboss.sbomer.core.service.rest.Page;
import org.jboss.sbomer.feature.sbom.core.model.Sbom;
import org.jboss.sbomer.feature.sbom.core.rest.RestUtils;

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
    Validator validator;

    public List<Sbom> searchByQuery(String rsqlQuery) {
        return sbomRepository.searchByQuery(rsqlQuery);
    }

    public Page<Sbom> searchByQueryPaginated(int pageIndex, int pageSize, String rsqlQuery) {
        return sbomRepository.searchByQueryPaginated(pageIndex, pageSize, rsqlQuery);
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
     * Delete the SBOM from the database.
     *
     * @param sbomId The {@link Sbom} id to delete from the database.
     */
    @Transactional
    public void deleteSbom(Long sbomId) {
        log.info("Deleting SBOM: {}", sbomId);

        // Update the SBOM field
        boolean deleted = sbomRepository.deleteById(sbomId);
        if (!deleted) {
            throw new NotFoundException("Could not find SBOM with ID '{}'", sbomId);
        }
    }

    /**
     * Delete the SBOM from the database.
     *
     * @param sbomId The {@link Sbom} id to delete from the database.
     */
    @Transactional
    public void deleteSbomWithBuildId(String buildId) {
        log.info("Deleting SBOMs with buildId: {}", buildId);

        // Update the SBOM field
        sbomRepository.deleteByBuildId(buildId);
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
