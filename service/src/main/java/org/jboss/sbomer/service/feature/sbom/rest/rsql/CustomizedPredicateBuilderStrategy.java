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
package org.jboss.sbomer.service.feature.sbom.rest.rsql;

import com.github.tennaito.rsql.builder.BuilderTools;
import com.github.tennaito.rsql.jpa.PredicateBuilder;
import com.github.tennaito.rsql.jpa.PredicateBuilderStrategy;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.Node;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;

import org.hibernate.query.criteria.internal.path.PluralAttributePath;
import org.hibernate.query.criteria.internal.path.SingularAttributePath;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;

public class CustomizedPredicateBuilderStrategy implements PredicateBuilderStrategy {

    @Override
    public <T> Predicate createPredicate(
            Node node,
            From root,
            Class<T> entity,
            EntityManager manager,
            BuilderTools tools) throws IllegalArgumentException {

        ComparisonNode cn = (ComparisonNode) node;
        ComparisonOperator operator = cn.getOperator();
        CriteriaBuilder builder = manager.getCriteriaBuilder();

        Path path = PredicateBuilder.findPropertyPath(cn.getSelector(), root, manager, tools);

        if (operator.equals(RSQLProducerImpl.IS_NULL)) {
            Object argument = cn.getArguments().get(0);
            if (argument instanceof String) {
                if (Boolean.parseBoolean((String) argument)) {
                    if (path instanceof SingularAttributePath) {
                        return builder.isNull(path);
                    } else if (path instanceof PluralAttributePath) {
                        return builder.isEmpty(path);
                    }
                } else {
                    if (path instanceof SingularAttributePath) {
                        return builder.isNotNull(path);
                    } else if (path instanceof PluralAttributePath) {
                        return builder.isNotEmpty(path);
                    }
                }
            }
        } else if (operator.equals(RSQLProducerImpl.IS_EQUAL)) {
            Object argument = cn.getArguments().get(0);
            if (argument instanceof String) {

                if (Enum.class.isAssignableFrom(path.getJavaType())) {
                    if (path.getJavaType().equals(GenerationResult.class)) {
                        try {
                            GenerationResult result = GenerationResult.valueOf(((String) argument).toUpperCase());
                            return builder.equal(path, result);
                        } catch (IllegalArgumentException ex) {
                            throw new IllegalArgumentException("Unknown value: " + argument + " for GenerationResult");
                        }
                    } else if (path.getJavaType().equals(SbomGenerationStatus.class)) {
                        try {
                            SbomGenerationStatus status = SbomGenerationStatus.fromName((String) argument);
                            return builder.equal(path, status);
                        } catch (IllegalArgumentException ex) {
                            throw new IllegalArgumentException(
                                    "Unknown value: " + argument + " for SbomGenerationStatus");
                        }
                    }
                }
                return builder.equal(path, argument);
            } else if (argument == null) {
                return builder.isNull(path);
            }
        }

        throw new IllegalArgumentException("Unknown operator: " + cn.getOperator());

    }

}