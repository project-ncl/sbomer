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
package org.jboss.sbomer.service.feature.sbom.rest.criteria.predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.pnc.common.Strings;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.rest.criteria.CriteriaAwareRepository;

import com.github.tennaito.rsql.builder.BuilderTools;
import com.github.tennaito.rsql.jpa.PredicateBuilder;
import com.github.tennaito.rsql.misc.EntityManagerAdapter;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.Node;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomPredicateSortBuilder<T> {

    public static <T> Collection<Order> createExpression(
            LogicalNode logical,
            Root<?> root,
            Class<T> entity,
            EntityManagerAdapter entityManager,
            BuilderTools misc) {

        log.debug("Creating Order for logical node: {}", logical);

        List<Order> orders = new ArrayList<Order>();

        for (Node node : logical.getChildren()) {

            log.debug("Creating Orders from all children nodes.");

            if (node instanceof LogicalNode) {
                orders.addAll(createExpression((LogicalNode) node, root, entity, entityManager, misc));
            } else if (node instanceof ComparisonNode) {
                orders.addAll(createExpression((ComparisonNode) node, root, entity, entityManager, misc));
            } else {
                throw new IllegalArgumentException("Unknown expression type: " + node.getClass());
            }
        }

        return orders;
    }

    /**
     * Create an Order from the RSQL AST comparison node.
     *
     * @param comparison RSQL AST comparison node.
     * @param startRoot From that predicate expression paths depends on.
     * @param entity The main entity of the query.
     * @param entityManager JPA EntityManager.
     * @param misc Facade with all necessary tools for predicate creation.
     * @return Order a order representation of the Node.
     */
    public static <T> Collection<Order> createExpression(
            ComparisonNode comparison,
            Root<?> startRoot,
            Class<T> entity,
            EntityManagerAdapter entityManager,
            BuilderTools misc) {

        log.debug("Creating Order for comparison node: {}", comparison);

        if (startRoot == null) {
            String msg = "From root node was undefined.";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (startRoot.getJavaType().equals(Sbom.class) && comparison.getSelector().equals("sbom")) {
            String msg = "RSQL sorting on field Sbom.sbom with type JsonNode is not supported!";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (startRoot.getJavaType().equals(SbomGenerationRequest.class) && comparison.getSelector().equals("config")) {
            String msg = "RSQL sorting on field SbomGenerationRequest.config with type JsonNode is not supported!";
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (CriteriaAwareRepository.ASC.equals(comparison.getOperator())
                || CriteriaAwareRepository.DESC.equals(comparison.getOperator())) {

            return createOrder(comparison, startRoot, entity, entityManager, misc);
        }

        throw new IllegalArgumentException("Unknown comparison operator used for sorting " + comparison.getOperator());
    }

    public static <T> Collection<Order> createOrder(
            Node node,
            Root<?> root,
            Class<T> entity,
            EntityManagerAdapter ema,
            BuilderTools tools) throws IllegalArgumentException {

        log.debug("Creating Order for comparison node: {}", node);

        ComparisonNode cn = (ComparisonNode) node;
        ComparisonOperator operator = cn.getOperator();
        CriteriaBuilder builder = ema.getCriteriaBuilder();

        Path<?> path = PredicateBuilder.findPropertyPath(cn.getSelector(), root, ema, tools);

        if (operator.equals(CriteriaAwareRepository.ASC)) {
            return List.of(builder.asc(path));
        } else if (operator.equals(CriteriaAwareRepository.DESC)) {
            return List.of(builder.desc(path));
        }

        throw new UnsupportedOperationException("Unsupported sorting: " + operator);
    }

    /*
     * Given sort like id=asc=,creationTime=desc=, return id=asc=id;creationTime=desc=creationTime This will make the
     * RSQL Parser happy about the sort strings
     */
    public static String rsqlParserCompliantSort(String sort) {
        if (!Strings.isEmpty(sort)) {
            StringBuilder compliantSort = new StringBuilder();
            // OR (,) and AND (;) are meaningless in order sorting, can be interchanged. Using just one of them for
            // simplicity
            sort = sort.replaceAll(",", ";");
            String[] sortTokens = sort.split(";");
            for (int i = 0; i < sortTokens.length; i++) {
                var token = sortTokens[i];
                if (token.contains(CriteriaAwareRepository.ASC.getSymbol())) {
                    token = token.replaceFirst(CriteriaAwareRepository.ASC.getSymbol(), "");
                    compliantSort.append(token).append(CriteriaAwareRepository.ASC.getSymbol()).append(token);
                } else if (token.contains(CriteriaAwareRepository.DESC.getSymbol())) {
                    token = token.replaceFirst(CriteriaAwareRepository.DESC.getSymbol(), "");
                    compliantSort.append(token).append(CriteriaAwareRepository.DESC.getSymbol()).append(token);
                } else {
                    compliantSort.append(token);
                }
                if (i < sortTokens.length - 1) {
                    compliantSort.append(";");
                }
            }
            return compliantSort.toString();
        }
        return "";
    }

}
