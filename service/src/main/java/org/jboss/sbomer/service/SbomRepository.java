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

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.model.Sbom;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Parameters;

@ApplicationScoped
public class SbomRepository implements PanacheRepositoryBase<Sbom, Long> {

    public Sbom getSbom(String buildId, GeneratorImplementation generator) {
        return find(
                "select s from Sbom s where s.buildId = :buildId and s.generator = :generator and s.processors is empty",
                Parameters.with("buildId", buildId).and("generator", generator)).singleResult();
    }

    public Sbom getBaseSbomByBuildId(String buildId) {
        return find(
                "select s from Sbom s where s.buildId = :buildId and s.processors is empty",
                Parameters.with("buildId", buildId)).singleResult();
    }

    public Sbom getEnrichedSbomByBuildId(String buildId) {
        return find("#" + Sbom.FIND_ENRICHED_BY_BUILDID, buildId).singleResult();
    }

    public Sbom getBaseSbomByRootPurl(String purl) {
        return find("#" + Sbom.FIND_BASE_BY_ROOT_PURL, purl).singleResult();
    }

    public Sbom getEnrichedSbomByRootPurl(String purl) {
        return find("#" + Sbom.FIND_ENRICHED_BY_ROOT_PURL, purl).singleResult();
    }

    public PanacheQuery<Sbom> getAllSbomWithBuildIdQuery(String buildId) {
        return find("#" + Sbom.FIND_ALL_BY_BUILDID, buildId);
    }

    @Transactional
    public Sbom saveSbom(Sbom sbom) {
        persistAndFlush(sbom);
        return sbom;
    }

}
