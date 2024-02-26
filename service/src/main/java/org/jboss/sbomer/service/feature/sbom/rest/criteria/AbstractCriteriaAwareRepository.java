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
package org.jboss.sbomer.service.feature.sbom.rest.criteria;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.pnc.common.Strings;
import org.jboss.sbomer.service.feature.sbom.rest.criteria.predicate.CustomPredicateSortBuilder;
import org.jboss.sbomer.service.feature.sbom.rest.criteria.predicate.CustomizedJpaPredicateSortVisitor;
import org.jboss.sbomer.service.feature.sbom.rest.criteria.predicate.CustomizedJpaPredicateVisitor;
import org.jboss.sbomer.service.feature.sbom.rest.criteria.predicate.CustomizedPredicateBuilderStrategy;

import com.github.tennaito.rsql.misc.EntityManagerAdapter;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
public abstract class AbstractCriteriaAwareRepository<T> implements PanacheRepositoryBase<T, String> {
    protected static final RSQLParser predicateParser;
    protected static final RSQLParser sortParser;

    public final static ComparisonOperator IS_NULL = new ComparisonOperator("=isnull=");
    public final static ComparisonOperator IS_EQUAL = new ComparisonOperator("=eq=");

    public static final ComparisonOperator ASC = new ComparisonOperator("=asc=", true);
    public static final ComparisonOperator DESC = new ComparisonOperator("=desc=", true);

    CriteriaBuilder criteriaBuilder;
    EntityManagerAdapter entityManagerAdapter;
    Class<T> entityType;

    static {
        Set<ComparisonOperator> predicateOperators = RSQLOperators.defaultOperators();
        predicateOperators.add(IS_NULL);
        predicateOperators.add(IS_EQUAL);

        predicateParser = new RSQLParser(predicateOperators);

        Set<ComparisonOperator> sortOperators = new HashSet<ComparisonOperator>();
        sortOperators.add(ASC);
        sortOperators.add(DESC);

        sortParser = new RSQLParser(sortOperators);
    }

    /**
     * Reads the total number of entities that satisfy the RSQL query.
     *
     * @param rsqlQuery RSQL query to be taken into account.
     * @return
     */
    public Long countByRsqlQuery(String rsqlQuery) {
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> root = criteriaQuery.from(entityType);
        criteriaQuery.select(criteriaBuilder.countDistinct(root));

        criteriaQuery = handleRsql(criteriaQuery, root, rsqlQuery);
        return getEntityManager().createQuery(criteriaQuery).getSingleResult();
    }

    public AbstractCriteriaAwareRepository(Class<T> entityType) {
        this.entityType = entityType;
    }

    @PostConstruct
    @Transactional
    protected void init() {
        entityManagerAdapter = new EntityManagerAdapter(getEntityManager());
        criteriaBuilder = entityManagerAdapter.getCriteriaBuilder();
    }

    protected <X> CriteriaQuery<X> handleRsql(CriteriaQuery<X> query, Root<T> root, String rsqlQuery) {

        if (Strings.isEmpty(rsqlQuery)) {
            return query;
        }

        log.debug("Applying provided RSQL query to enhance search: '{}'", rsqlQuery);

        // Create custom implementation of RSQLVisitor which converts nodes to predicates using
        // CustomizedPredicateBuilder or CustomizedPredicateBuilderStrategy for custom operators
        RSQLVisitor<Predicate, EntityManagerAdapter> visitor = new CustomizedJpaPredicateVisitor<X>().withRoot(root)
                .withPredicateBuilderStrategy(new CustomizedPredicateBuilderStrategy());

        // create RSQLParser with default and custom operators
        Node rootNode = predicateParser.parse(rsqlQuery);
        Predicate predicate = rootNode.accept(visitor, entityManagerAdapter);

        return query.where(predicate);
    }

    protected <X> CriteriaQuery<X> handleSort(CriteriaQuery<X> query, Root<T> root, String sort) {
        if (Strings.isEmpty(sort)) {
            return query;
        }

        String compliantSort = CustomPredicateSortBuilder.rsqlParserCompliantSort(sort);
        log.debug("Modified RSQL sort string from: '{}' to a RSQL parser compliant format: '{}'", sort, compliantSort);

        CustomizedJpaPredicateSortVisitor<?> sortVisitor = new CustomizedJpaPredicateSortVisitor<>(entityType)
                .withRoot(root);

        Node sortRootNode = sortParser.parse(compliantSort);
        Collection<Order> orders = sortVisitor.accept(sortRootNode, entityManagerAdapter);

        return query.orderBy(orders.toArray(new Order[orders.size()]));
    }

}
