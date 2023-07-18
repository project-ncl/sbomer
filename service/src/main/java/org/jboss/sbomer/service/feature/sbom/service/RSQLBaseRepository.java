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

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.criteria.CriteriaQuery;

import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.service.feature.sbom.rest.rsql.RSQLProducer;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public abstract class RSQLBaseRepository<T extends PanacheEntityBase, ID> implements PanacheRepositoryBase<T, ID> {

    @Inject
    RSQLProducer<T> rsqlProducer;

    public Page<T> searchByQueryPaginated(int pageIndex, int pageSize, String rsqlQuery, String sort) {
        log.debug(
                "Getting list of all {} with pageIndex: {}, pageSize: {}, rsqlQuery: {}, sort: {}",
                getEntityClass(),
                pageIndex,
                pageSize,
                rsqlQuery,
                sort);

        List<T> content = searchByQuery(pageIndex, pageSize, rsqlQuery, sort);
        Long totalHits = countByQuery(rsqlQuery);

        log.debug("Found content: {}, totalHits: {}", content, totalHits);

        int totalPages = 0;

        if (totalHits == 0) {
            totalPages = 1; // a single page of zero results
        } else {
            totalPages = (int) Math.ceil((double) totalHits / (double) pageSize);
        }

        return new Page<>(pageIndex, pageSize, totalPages, totalHits, content);
    }

    public List<T> searchByQuery(String rsqlQuery, String sort) {

        CriteriaQuery<T> query = rsqlProducer.getCriteriaQuery(getEntityClass(), rsqlQuery, sort);
        List<T> resultList = getEntityManager().createQuery(query).getResultList();
        if (resultList == null || resultList.isEmpty()) {
            return Collections.emptyList();
        }
        return resultList;
    }

    public List<T> searchByQuery(int pageIndex, int pageSize, String rsqlQuery, String sort) {

        CriteriaQuery<T> query = rsqlProducer.getCriteriaQuery(getEntityClass(), rsqlQuery, sort);
        List<T> resultList = getEntityManager().createQuery(query)
                .setFirstResult(pageIndex * pageSize)
                .setMaxResults(pageSize)
                .getResultList();
        if (resultList == null || resultList.isEmpty()) {
            return Collections.emptyList();
        }
        return resultList;
    }

    public Long countByQuery(String rsqlQuery) {

        CriteriaQuery<Long> query = rsqlProducer.getCountCriteriaQuery(getEntityClass(), rsqlQuery);
        return getEntityManager().createQuery(query).getSingleResult();
    }

    protected abstract Class<T> getEntityClass();

}
