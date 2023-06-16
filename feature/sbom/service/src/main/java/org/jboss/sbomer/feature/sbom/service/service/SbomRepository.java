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
package org.jboss.sbomer.feature.sbom.service.service;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaQuery;
import javax.transaction.Transactional;

import org.jboss.sbomer.core.errors.NotFoundException;
import org.jboss.sbomer.feature.sbom.core.enums.SbomStatus;
import org.jboss.sbomer.feature.sbom.service.model.Sbom;
import org.jboss.sbomer.feature.sbom.service.rest.Page;
import org.jboss.sbomer.feature.sbom.service.rest.rsql.RSQLProducer;

import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class SbomRepository implements PanacheRepositoryBase<Sbom, Long> {

    @Inject
    RSQLProducer<Sbom> rsqlProducer;

    public Page<Sbom> searchByQueryPaginated(int pageIndex, int pageSize, String rsqlQuery) {
        log.debug(
                "Getting list of all base SBOMS with pageIndex: {}, pageSize: {}, rsqlQuery: {}",
                pageIndex,
                pageSize,
                rsqlQuery);

        List<Sbom> content = searchByQuery(pageIndex, pageSize, rsqlQuery);
        Long totalHits = countByQuery(rsqlQuery);

        log.debug("Found content: {}, totalHits: {}", content, totalHits);

        int totalPages = 0;

        if (totalHits == 0) {
            totalPages = 1; // a single page of zero results
        } else {
            totalPages = (int) Math.ceil((double) totalHits / (double) pageSize);
        }

        return new Page<Sbom>(pageIndex, pageSize, totalPages, totalHits, content);
    }

    public List<Sbom> searchByQuery(String rsqlQuery) {

        CriteriaQuery<Sbom> query = rsqlProducer.getCriteriaQuery(Sbom.class, rsqlQuery);
        List<Sbom> resultList = getEntityManager().createQuery(query).getResultList();
        if (resultList == null || resultList.isEmpty()) {
            return Collections.emptyList();
        }
        return resultList;
    }

    public List<Sbom> searchByQuery(int pageIndex, int pageSize, String rsqlQuery) {

        CriteriaQuery<Sbom> query = rsqlProducer.getCriteriaQuery(Sbom.class, rsqlQuery);
        List<Sbom> resultList = getEntityManager().createQuery(query)
                .setFirstResult(pageIndex * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
        if (resultList == null || resultList.isEmpty()) {
            return Collections.emptyList();
        }
        return resultList;
    }

    public Long countByQuery(String rsqlQuery) {

        CriteriaQuery<Long> query = rsqlProducer.getCountCriteriaQuery(Sbom.class, rsqlQuery);
        return getEntityManager().createQuery(query).getSingleResult();
    }

    @Transactional
    public void deleteByBuildId(String buildId) {

        // Do not delete SBOMs which are being processed
        String query = "FROM Sbom WHERE buildId = :buildId AND status <> :status";
        List<Sbom> sboms = getEntityManager().createQuery(query, Sbom.class)
                .setParameter("buildId", buildId)
                .setParameter("status", SbomStatus.IN_PROGRESS)
                .getResultList();

        if (sboms == null) {
            throw new NotFoundException("Could not find any final SBOM with buildId '{}'", buildId);
        }

        sboms.stream().filter(s -> s.getParentSbom() != null).forEach(s -> {
            if (s.getProcessors() != null) {
                s.getProcessors().clear();
            }

            getEntityManager().remove(s);
            getEntityManager().flush();
        });

        sboms.stream().filter(s -> s.getParentSbom() == null).forEach(s -> {
            if (s.getProcessors() != null) {
                s.getProcessors().clear();
            }

            getEntityManager().remove(s);
            getEntityManager().flush();
        });

    }

    @Transactional
    public Sbom saveSbom(Sbom sbom) {
        persistAndFlush(sbom);
        return sbom;
    }

}
