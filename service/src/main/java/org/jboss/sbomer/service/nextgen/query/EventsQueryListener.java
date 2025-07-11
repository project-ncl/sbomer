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
package org.jboss.sbomer.service.nextgen.query;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.jboss.sbomer.service.nextgen.antlr.QueryBaseListener;
import org.jboss.sbomer.service.nextgen.antlr.QueryParser;
import org.jboss.sbomer.service.nextgen.antlr.QueryParser.PredicateContext;
import org.jboss.sbomer.service.nextgen.core.enums.EventStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * An ANTLR Listener that traverses the parsed query tree and builds a JPQL
 * (Java Persistence Query Language) `WHERE`
 * clause along with a map of named parameters. This is designed to be used with
 * Panache queries.
 * </p>
 *
 * <p>
 * It uses a stack to construct the query string as the
 * {@link org.antlr.v4.runtime.tree.ParseTreeWalker} fires
 * enter/exit events.
 * </p>
 */
@Slf4j
public class EventsQueryListener extends QueryBaseListener {

    private final Stack<String> queryParts = new Stack<>();
    private final Map<String, Object> parameters = new HashMap<>();
    private int paramIndex = 0;

    /**
     * Returns the final, fully constructed JPQL WHERE clause.
     *
     * @return The JPQL WHERE clause string.
     */
    public String getJpqlWhereClause() {
        if (queryParts.isEmpty()) {
            return "";
        }
        return queryParts.peek();
    }

    /**
     * Returns the map of named parameters collected during the tree traversal.
     *
     * @return A map where keys are parameter names (e.g., "param0") and values are
     *         the corresponding query values.
     */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public void enterQuery(QueryParser.QueryContext ctx) {
        log.info("Entering Query: '{}'", ctx.getText());
    }

    @Override
    public void exitQuery(QueryParser.QueryContext ctx) {
        log.info("Exiting Query: '{}'", ctx.getText());
    }

    @Override
    public void enterExpression(QueryParser.ExpressionContext ctx) {
        log.info("  -> Entering Expression: '{}'", ctx.getText());
    }

    @Override
    public void exitExpression(QueryParser.ExpressionContext ctx) {
        log.info("  <- Exiting Expression: '{}'", ctx.getText());

        // If the expression is a logical AND or OR, its two operands will be on the top
        // of the stack.
        if (ctx.AND() != null || ctx.OR() != null) {
            // The right-hand side was visited last, so it's on top.
            String right = queryParts.pop();
            String left = queryParts.pop();
            String op = ctx.AND() != null ? " AND " : " OR ";

            // Combine them, wrap in parentheses, and push the result back.
            queryParts.push("(" + left + op + right + ")");
        }
        // If it's a parenthesized expression, like `(id = "A")`, the inner result `id =
        // :param0` is already on the
        // stack. We just wrap it in parentheses.
        else if (ctx.LPAREN() != null) {
            String expr = queryParts.pop();
            queryParts.push("(" + expr + ")");
        } else if (ctx.predicate() == null) {
            // If it's not a logical expression, not a parenthesized expression,
            // and not a simple predicate, then it's an unsupported operation.
            throw new UnsupportedOperationException("Unsupported operator in expression: " + ctx.getText());
        }
    }

    @Override
    public void enterPredicate(QueryParser.PredicateContext ctx) {
        log.info("    -> Entering Predicate: '{}'", ctx.getText());
    }

    @Override
    public void exitPredicate(QueryParser.PredicateContext ctx) {
        log.info("    <- Exiting Predicate: '{}'", ctx.getText());

        String field = ctx.IDENTIFIER().getText();
        String operator = getOperator(ctx);

        validatePredicate(field, operator);

        String stringValue = parseValue(ctx.value());

        Object finalValue = convertValue(field, stringValue);

        if (" LIKE ".equals(operator)) {
            finalValue = "%" + finalValue + "%";
        }

        String paramName = "param" + paramIndex++;
        parameters.put(paramName, finalValue);
        queryParts.push(field + operator + ":" + paramName);
    }

    @Override
    public void enterValue(QueryParser.ValueContext ctx) {
        log.info("      -> Entering Value: '{}'", ctx.getText());
    }

    @Override
    public void exitValue(QueryParser.ValueContext ctx) {
        log.info("      <- Exiting Value: '{}'", ctx.getText());
    }

    /**
     * Converts the raw string value from the query into the correct Java type based
     * on the field name.
     * Also performs validation for the value format.
     *
     * @param field       The field name being queried.
     * @param stringValue The raw string value from the query.
     * @return A correctly typed object (e.g., Instant, Long, Enum).
     */
    private Object convertValue(String field, String stringValue) {
        switch (field) {
            case "status":
                try {
                    return EventStatus.valueOf(stringValue.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid value for field 'status'. Valid values are: "
                                    + Arrays.toString(EventStatus.values()));
                }
            case "created":
            case "updated":
            case "finished":
                try {
                    return Instant.parse(stringValue);
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException(
                            "Invalid timestamp format for field '" + field
                                    + "'. Expected ISO-8601 format (e.g., \"2023-01-01T12:00:00Z\").");
                }
            case "id":
            case "reason":
                return stringValue;
            default:
                throw new IllegalArgumentException("Unknown field: '" + field + "'");
        }
    }

    /**
     * Validates that the operator is compatible with the field type.
     *
     * @param field    The field name being queried.
     * @param operator The operator being used.
     */
    private void validatePredicate(String field, String operator) {
        switch (field) {
            case "id":
            case "reason":
                if (operator.equals(" > ") || operator.equals(" < ") || operator.equals(" >= ")
                        || operator.equals(" <= ")) {
                    throw new UnsupportedOperationException(
                            "Operator '" + operator.trim() + "' is not supported for string field '" + field + "'");
                }
                break;
            case "created":
            case "updated":
            case "finished":
                if (operator.equals(" LIKE ")) {
                    throw new UnsupportedOperationException(
                            "Operator 'LIKE' is not supported for numeric or date field '" + field + "'");
                }
                break;
        }
    }

    private String getOperator(PredicateContext ctx) {
        if (ctx.EQUAL() != null)
            return " = ";
        if (ctx.NOT_EQUAL() != null)
            return " != ";
        if (ctx.GREATER_THAN() != null)
            return " > ";
        if (ctx.LESS_THAN() != null)
            return " < ";
        if (ctx.GREATER_THAN_OR_EQUAL() != null)
            return " >= ";
        if (ctx.LESS_THAN_OR_EQUAL() != null)
            return " <= ";
        if (ctx.CONTAINS() != null)
            return " LIKE ";
        throw new UnsupportedOperationException("Operator not implemented: " + ctx.getText());
    }

    private String parseValue(QueryParser.ValueContext valueCtx) {
        String rawValue = valueCtx.STRING().getText();
        // The STRING has ALWAYS quotes around it per the grammar
        return rawValue.substring(1, rawValue.length() - 1);
    }
}
