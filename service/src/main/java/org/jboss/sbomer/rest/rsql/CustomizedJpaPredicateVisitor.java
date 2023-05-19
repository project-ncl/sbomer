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

import com.github.tennaito.rsql.jpa.AbstractJpaVisitor;
import com.github.tennaito.rsql.jpa.PredicateBuilderStrategy;
import com.github.tennaito.rsql.misc.ArgumentParser;
import com.github.tennaito.rsql.misc.Mapper;
import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.OrNode;
import lombok.extern.slf4j.Slf4j;

import javax.persistence.EntityManager;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Predicate;

@Slf4j
public class CustomizedJpaPredicateVisitor<T> extends AbstractJpaVisitor<Predicate, T> {
    private From root;
    private CustomPredicateBuilder<T> customizedPredicateBuilder;

    public CustomizedJpaPredicateVisitor<T> withRoot(From root) {
        this.root = root;
        return this;
    }

    public CustomizedJpaPredicateVisitor<T> withPredicateBuilder(CustomPredicateBuilder builder) {
        this.customizedPredicateBuilder = builder;
        return this;
    }

    public CustomizedJpaPredicateVisitor<T> withMapper(Mapper mapper) {
        this.getBuilderTools().setPropertiesMapper(mapper);
        return this;
    }

    public CustomizedJpaPredicateVisitor<T> withArgumentParser(ArgumentParser argumentParser) {
        this.getBuilderTools().setArgumentParser(argumentParser);
        return this;
    }

    public CustomizedJpaPredicateVisitor<T> withPredicateBuilderStrategy(PredicateBuilderStrategy strategy) {
        this.getBuilderTools().setPredicateBuilder(strategy);
        return this;
    }

    @Override
    public Predicate visit(AndNode node, EntityManager em) {
        log.info("visit: AndNode {}", node);
        return customizedPredicateBuilder.createPredicate(node, root, entityClass, em, getBuilderTools());
    }

    @Override
    public Predicate visit(OrNode node, EntityManager em) {
        log.info("visit: OrNode {}", node);
        return customizedPredicateBuilder.createPredicate(node, root, entityClass, em, getBuilderTools());
    }

    @Override
    public Predicate visit(ComparisonNode node, EntityManager em) {
        log.info("visit: ComparisonNode {}", node);
        return customizedPredicateBuilder.createPredicate(node, root, entityClass, em, getBuilderTools());
    }

}
