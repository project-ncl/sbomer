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
package org.jboss.sbomer.repositories;

import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;

import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.utils.enums.Processors;

import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;

@ApplicationScoped
public class SbomRepository implements PanacheRepositoryBase<Sbom, Long> {

    public Sbom getSbom(String buildId, GeneratorImplementation generator, Processors processor) {
        if (processor == null) {
            return find("#" + Sbom.FIND_BASE_BY_BUILDID_GENERATOR, buildId, generator).singleResult();
        }
        return find("#" + Sbom.FIND_BY_BUILDID_GENERATOR_PROCESSOR, buildId, generator, processor).singleResult();
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