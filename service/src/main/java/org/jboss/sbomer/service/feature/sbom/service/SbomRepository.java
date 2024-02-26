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

import java.time.Instant;
import java.util.List;

import org.jboss.sbomer.core.dto.v1alpha2.SbomRecord;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.rest.QueryParameters;
import org.jboss.sbomer.service.feature.sbom.rest.criteria.CriteriaAwareRepository;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.criteria.Join;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SbomRepository extends CriteriaAwareRepository<Sbom> {

    public SbomRepository() {
        super(Sbom.class);
    }

    public List<SbomRecord> searchSbomRecords(QueryParameters parameters) {
        // TODO: Implement strong typing
        return searchProjected(SbomRecord.class, parameters, (query, builder, root) -> {
            Join<Sbom, SbomGenerationRequest> generationRequest = root.join("generationRequest");

            return query.select(
                    builder.construct(
                            SbomRecord.class,
                            root.<String> get("id"),
                            root.<String> get("identifier"),
                            root.<String> get("rootPurl"),
                            root.<Instant> get("creationTime"),
                            root.<Integer> get("configIndex"),
                            root.<String> get("statusMessage"),
                            generationRequest.<String> get("id"),
                            generationRequest.<String> get("identifier").alias("gIdentifier"),
                            generationRequest.<JsonNode> get("config"),
                            generationRequest.<GenerationRequestType> get("type").as(String.class),
                            generationRequest.<Instant> get("creationTime"))

            );

        });
    }

    @Transactional
    public Sbom saveSbom(Sbom sbom) {
        persistAndFlush(sbom);
        return sbom;
    }

}
