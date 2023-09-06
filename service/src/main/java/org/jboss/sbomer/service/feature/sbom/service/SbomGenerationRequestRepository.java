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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;

import io.quarkus.panache.common.Parameters;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class SbomGenerationRequestRepository extends RSQLBaseRepository<SbomGenerationRequest, String> {

    protected Class getEntityClass() {
        return SbomGenerationRequest.class;
    }

    @Transactional
    public void deleteRequest(String id) {

        log.info("Deleting SbomGenerationRequest with id: '{}' and all its associated SBOMs", id);
        SbomGenerationRequest request = findById(id);
        if (request == null) {
            throw new NotFoundException("Could not find any SBOM generation request with id '{}'", id);
        }

        long sbomsDeletedCount = Sbom.delete("generationRequest.id = :id", Parameters.with("id", id));
        log.info("Deleted {} SBOMs associated with the Generation Request with id: '{}'", sbomsDeletedCount, id);

        SbomGenerationRequest.delete("id = :id", Parameters.with("id", id));
        log.info("Deleted the SBOM Generation Request with id: '{}'", id);

        flush();
    }

}
