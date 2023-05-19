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
package org.jboss.sbomer.rest.rsql;

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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomPredicateBuilder<T> {

    public static <T> Predicate createPredicate(
            Node node,
            From root,
            Class<T> entity,
            EntityManager manager,
            BuilderTools misc) {
        log.info("Creating Predicate for: {}", node);

        if (node instanceof LogicalNode) {
            return createPredicate((LogicalNode) node, root, entity, manager, misc);
        }

        if (node instanceof ComparisonNode) {
            return createPredicate((ComparisonNode) node, root, entity, manager, misc);
        }

        throw new IllegalArgumentException("Unknown expression type: " + node.getClass());
    }

    public static <T> Predicate createPredicate(
            LogicalNode logical,
            From root,
            Class<T> entity,
            EntityManager entityManager,
            BuilderTools misc) {
        log.info("Creating Predicate for logical node: {}", logical);

        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        List<Predicate> predicates = new ArrayList<Predicate>();

        log.info("Creating Predicates from all children nodes.");
        for (Node node : logical.getChildren()) {
            predicates.add(createPredicate(node, root, entity, entityManager, misc));
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

        if (startRoot == null) {
            String msg = "From root node was undefined.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (comparison.getSelector().equals("sbom")) {
            String msg = "RSQL on field Sbom.sbom with type JsonNode is not supported!";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        Path propertyPath = PredicateBuilder.findPropertyPath(comparison.getSelector(), startRoot, entityManager, misc);

        // If the operator is a IS_NULL type and type is an Enum or a PluralAttributePath (from ElementCollection),
        // delegate to our custom PredicateBuilderStrategy
        if (RSQLProducerImpl.IS_NULL.equals(comparison.getOperator())
                && (Enum.class.isAssignableFrom(propertyPath.getJavaType())
                        || propertyPath instanceof PluralAttributePath)) {

            log.info(
                    "Detected type {} for selector {} with custom comparison operator {}. Delegating to custom PredicateBuilderStrategy!",
                    propertyPath.getJavaType(),
                    comparison.getSelector(),
                    RSQLProducerImpl.IS_NULL.getSymbol());
            if (misc.getPredicateBuilder() != null) {
                return misc.getPredicateBuilder().createPredicate(comparison, startRoot, entity, entityManager, misc);
            }
        }

        return PredicateBuilder.createPredicate(comparison, startRoot, entity, entityManager, misc);
    }

}
