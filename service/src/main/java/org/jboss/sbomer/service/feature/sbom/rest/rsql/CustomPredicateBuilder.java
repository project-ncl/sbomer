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
package org.jboss.sbomer.service.feature.sbom.rest.rsql;

import com.github.tennaito.rsql.builder.BuilderTools;
import com.github.tennaito.rsql.jpa.PredicateBuilder;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.internal.path.PluralAttributePath;
import org.hibernate.query.criteria.internal.path.SingularAttributePath;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomPredicateBuilder<T> {

    public static <T> Predicate createPredicate(
            LogicalNode logical,
            From root,
            Class<T> entity,
            EntityManager entityManager,
            BuilderTools misc) {

        log.debug("Creating Predicate for logical node: {}", logical);

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        List<Predicate> predicates = new ArrayList<Predicate>();

        for (Node node : logical.getChildren()) {

            log.debug("Creating Predicates from all children nodes.");

            if (node instanceof LogicalNode) {
                predicates.add(createPredicate((LogicalNode) node, root, entity, entityManager, misc));
            } else if (node instanceof ComparisonNode) {
                predicates.add(createPredicate((ComparisonNode) node, root, entity, entityManager, misc));
            } else {
                throw new IllegalArgumentException("Unknown expression type: " + node.getClass());
            }
        }

        switch (logical.getOperator()) {
            case AND:
                return builder.and(predicates.toArray(new Predicate[predicates.size()]));
            case OR:
                return builder.or(predicates.toArray(new Predicate[predicates.size()]));
        }

        throw new IllegalArgumentException("Unknown operator: " + logical.getOperator());
    }

    /**
     * Create a Predicate from the RSQL AST comparison node.
     *
     * @param comparison RSQL AST comparison node.
     * @param startRoot From that predicate expression paths depends on.
     * @param entity The main entity of the query.
     * @param entityManager JPA EntityManager.
     * @param misc Facade with all necessary tools for predicate creation.
     * @return Predicate a predicate representation of the Node.
     */
    public static <T> Predicate createPredicate(
            ComparisonNode comparison,
            From startRoot,
            Class<T> entity,
            EntityManager entityManager,
            BuilderTools misc) {

        log.debug("Creating Predicate for comparison node: {}", comparison);

        if (startRoot == null) {
            String msg = "From root node was undefined.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (startRoot.getJavaType().equals(Sbom.class) && comparison.getSelector().equals("sbom")) {
            String msg = "RSQL on field Sbom.sbom with type JsonNode is not supported!";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (startRoot.getJavaType().equals(SbomGenerationRequest.class) && comparison.getSelector().equals("config")) {
            String msg = "RSQL on field SbomGenerationRequest.config with type JsonNode is not supported!";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        Path propertyPath = PredicateBuilder.findPropertyPath(comparison.getSelector(), startRoot, entityManager, misc);

        if ((RSQLProducerImpl.IS_NULL.equals(comparison.getOperator())
                && (Enum.class.isAssignableFrom(propertyPath.getJavaType())
                        || propertyPath instanceof PluralAttributePath))
                || (RSQLProducerImpl.IS_EQUAL.equals(comparison.getOperator())
                        && Enum.class.isAssignableFrom(propertyPath.getJavaType())
                        && propertyPath instanceof SingularAttributePath)) {

            log.debug(
                    "Detected type '{}' for selector '{}' with custom comparison operator '{}'. Delegating to custom PredicateBuilderStrategy!",
                    propertyPath.getJavaType(),
                    comparison.getSelector(),
                    comparison.getOperator().getSymbol());

            if (misc.getPredicateBuilder() != null) {
                return misc.getPredicateBuilder().createPredicate(comparison, startRoot, entity, entityManager, misc);
            }
        }

        return PredicateBuilder.createPredicate(comparison, startRoot, entity, entityManager, misc);
    }

}
