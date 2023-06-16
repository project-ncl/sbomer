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
package org.jboss.sbomer.feature.sbom.service.rest.rsql;

import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

import org.jboss.pnc.common.Strings;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

@ApplicationScoped
public class RSQLProducerImpl<T> implements RSQLProducer<T> {

    private final RSQLParser predicateParser;
    private final RSQLParser sortParser;

    @Inject
    EntityManager entityManager;

    public final static ComparisonOperator IS_NULL = new ComparisonOperator("=isnull=");
    public final static ComparisonOperator IS_EQUAL = new ComparisonOperator("=eq=");

    static final ComparisonOperator ASC = new ComparisonOperator("=asc=", true);
    static final ComparisonOperator DESC = new ComparisonOperator("=desc=", true);

    public RSQLProducerImpl() {
        Set<ComparisonOperator> predicateOperators = RSQLOperators.defaultOperators();
        predicateOperators.add(IS_NULL);
        predicateOperators.add(IS_EQUAL);

        predicateParser = new RSQLParser(predicateOperators);

        Set<ComparisonOperator> sortOperators = new HashSet<ComparisonOperator>();
        sortOperators.add(ASC);
        sortOperators.add(DESC);

        sortParser = new RSQLParser(sortOperators);
    }

    @Override
    public CriteriaQuery<T> getCriteriaQuery(Class<T> type, String rsqlQuery) {

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> criteria = builder.createQuery(type);
        From root = criteria.from(type);

        if (Strings.isEmpty(rsqlQuery)) {
            return criteria.where(builder.conjunction());
        }

        // Create custom implementation of RSQLVisitor which converts nodes to predicates using
        // CustomizedPredicateBuilder or CustomizedPredicateBuilderStrategy for custom operators
        RSQLVisitor<Predicate, EntityManager> visitor = new CustomizedJpaPredicateVisitor<T>().withRoot(root)
                .withPredicateBuilder(new CustomPredicateBuilder<T>())
                .withPredicateBuilderStrategy(new CustomizedPredicateBuilderStrategy());

        // create RSQLParser with default and custom operators
        Node rootNode = predicateParser.parse(rsqlQuery);
        Predicate predicate = rootNode.accept(visitor, entityManager);

        return criteria.where(predicate);
    }

    public CriteriaQuery<Long> getCountCriteriaQuery(Class<T> type, String rsqlQuery) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        From root = criteria.from(type);
        criteria.select(builder.countDistinct(root));

        if (Strings.isEmpty(rsqlQuery)) {
            return criteria.where(builder.conjunction());
        }

        // Create custom implementation of RSQLVisitor which converts nodes to predicates using
        // CustomizedPredicateBuilder or CustomizedPredicateBuilderStrategy for custom operators
        RSQLVisitor<Predicate, EntityManager> visitor = new CustomizedJpaPredicateVisitor<T>().withRoot(root)
                .withPredicateBuilder(new CustomPredicateBuilder<T>())
                .withPredicateBuilderStrategy(new CustomizedPredicateBuilderStrategy());

        // create RSQLParser with default and custom operators
        Node rootNode = predicateParser.parse(rsqlQuery);
        Predicate predicate = rootNode.accept(visitor, entityManager);

        return criteria.where(predicate);
    }

}
