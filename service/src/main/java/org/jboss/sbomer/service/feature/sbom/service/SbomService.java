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
package org.jboss.sbomer.service.feature.sbom.service;

import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.core.errors.ValidationException;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.rest.Page;
import org.jboss.sbomer.service.feature.sbom.rest.RestUtils;

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
    SbomGenerationRequestRepository sbomRequestRepository;

    @Inject
    Validator validator;

    public List<Sbom> searchSbomsByQuery(String rsqlQuery) {
        return sbomRepository.searchByQuery(rsqlQuery);
    }

    public Page<Sbom> searchSbomsByQueryPaginated(int pageIndex, int pageSize, String rsqlQuery) {
        return sbomRepository.searchByQueryPaginated(pageIndex, pageSize, rsqlQuery);
    }

    public List<SbomGenerationRequest> searchSbomRequestsByQuery(String rsqlQuery) {
        return sbomRequestRepository.searchByQuery(rsqlQuery);
    }

    public Page<SbomGenerationRequest> searchSbomRequestsByQueryPaginated(
            int pageIndex,
            int pageSize,
            String rsqlQuery) {
        return sbomRequestRepository.searchByQueryPaginated(pageIndex, pageSize, rsqlQuery);
    }

    /**
     * Returns {@link Sbom} for the specified identifier.
     *
     * @param sbomId As {@link String}.
     * @return The {@link Sbom} object.
     */
    public Sbom get(String sbomId) {
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
    public Sbom updateBom(String sbomId, JsonNode bom) {
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
     * Delete the SBOM Generation Request and all its associated SBOMs from the database.
     *
     * @param id The {@link SbomGenerationRequest} id to delete from the database.
     */
    @Transactional
    public void deleteSbomRequest(String id) {
        log.info("Deleting SBOM Generation Request: {}", id);

        sbomRequestRepository.deleteRequest(id);
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
}
