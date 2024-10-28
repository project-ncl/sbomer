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
package org.jboss.sbomer.service.rest.criteria;

import java.util.List;

import org.jboss.sbomer.core.TriFunction;
import org.jboss.sbomer.service.rest.QueryParameters;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

public class CriteriaAwareRepository<T extends PanacheEntityBase> extends AbstractCriteriaAwareRepository<T> {

    public CriteriaAwareRepository(Class<T> entityType) {
        super(entityType);
    }

    public List<T> search(
            QueryParameters parameters,
            TriFunction<CriteriaQuery<T>, CriteriaBuilder, Root<T>, CriteriaQuery<T>> consumer) {

        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(entityType);

        return handleQuery(entityType, criteriaQuery, parameters, consumer);
    }

    protected <X> List<X> handleQuery(
            Class<X> targetType,
            CriteriaQuery<X> criteriaQuery,
            QueryParameters parameters,
            TriFunction<CriteriaQuery<X>, CriteriaBuilder, Root<T>, CriteriaQuery<X>> consumer) {
        return handleQuery(targetType, criteriaQuery, parameters, consumer, false);
    }

    protected <X> List<X> handleQuery(
            Class<X> targetType,
            CriteriaQuery<X> criteriaQuery,
            QueryParameters parameters,
            TriFunction<CriteriaQuery<X>, CriteriaBuilder, Root<T>, CriteriaQuery<X>> consumer,
            boolean count) {

        Root<T> root = criteriaQuery.from(entityType);

        if (consumer != null) {
            criteriaQuery = consumer.apply(criteriaQuery, criteriaBuilder, root);
        }

        criteriaQuery = handleRsql(criteriaQuery, root, parameters.getRsqlQuery());
        criteriaQuery = handleSort(criteriaQuery, root, parameters.getSort());

        TypedQuery<X> typedQuery = getEntityManager().createQuery(criteriaQuery);

        typedQuery.setFirstResult(parameters.firstResult());
        typedQuery.setMaxResults(parameters.maxResults());

        return typedQuery.getResultList();
    }

    public List<T> search(QueryParameters parameters) {
        return search(parameters, null);
    }

    public List<T> search() {
        return search(new QueryParameters(), null);
    }

    public <Z> List<Z> searchProjected(
            Class<Z> recordType,
            QueryParameters parameters,
            TriFunction<CriteriaQuery<Z>, CriteriaBuilder, Root<T>, CriteriaQuery<Z>> consumer) {

        CriteriaQuery<Z> criteriaQuery = criteriaBuilder.createQuery(recordType);

        return handleQuery(recordType, criteriaQuery, parameters, consumer);
    }
}
