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
import com.github.tennaito.rsql.builder.SimpleBuilderTools;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

import javax.persistence.EntityManager;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Order;

@Slf4j
public class CustomizedJpaPredicateSortVisitor<T> implements RSQLVisitor<Collection<Order>, EntityManager> {

    private From root;
    protected Class<T> entityClass;
    protected BuilderTools builderTools;

    /**
     * Constructor with template varargs for entityClass discovery.
     *
     * @param e not for usage
     */
    public CustomizedJpaPredicateSortVisitor(T... type) {
        // getting class from template... :P
        if (type.length == 0) {
            entityClass = (Class<T>) type.getClass().getComponentType();
        } else {
            entityClass = (Class<T>) type[0].getClass();
        }
    }

    public CustomizedJpaPredicateSortVisitor<T> withRoot(From root) {
        this.root = root;
        return this;
    }

    @Override
    public Collection<Order> visit(AndNode node, EntityManager em) {
        log.trace("visit: AndNode {}", node);
        return CustomPredicateSortBuilder.createExpression(node, root, entityClass, em, getBuilderTools());
    }

    @Override
    public Collection<Order> visit(OrNode node, EntityManager em) {
        log.trace("visit: OrNode {}", node);
        return CustomPredicateSortBuilder.createExpression(node, root, entityClass, em, getBuilderTools());
    }

    @Override
    public Collection<Order> visit(ComparisonNode node, EntityManager em) {
        log.trace("visit: ComparisonNode {}", node);
        return CustomPredicateSortBuilder.createExpression(node, root, entityClass, em, getBuilderTools());
    }

    public Collection<Order> accept(Node node, EntityManager em) {
        if (node instanceof AndNode) {
            return visit((AndNode) node, em);
        } else if (node instanceof OrNode) {
            return visit((OrNode) node, em);
        } else {
            return visit((ComparisonNode) node, em);
        }
    }

    /**
     * Get builder tools.
     *
     * @return BuilderTools.
     */
    public BuilderTools getBuilderTools() {
        if (this.builderTools == null) {
            this.builderTools = new SimpleBuilderTools();
        }
        return this.builderTools;
    }

    /**
     * Set a predicate strategy.
     *
     * @param delegate PredicateBuilderStrategy.
     */
    public void setBuilderTools(BuilderTools delegate) {
        this.builderTools = delegate;
    }

}
