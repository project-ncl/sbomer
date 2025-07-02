package org.jboss.sbomer.service.nextgen.query;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.jboss.sbomer.service.nextgen.antlr4.antlr.QueryBaseListener;
import org.jboss.sbomer.service.nextgen.antlr4.antlr.QueryParser;
import org.jboss.sbomer.service.nextgen.antlr4.antlr.QueryParser.PredicateContext;

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
public class JpqlQueryListener extends QueryBaseListener {

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
        // This is the core logic for the listener. It fires after all children of an
        // expression have been processed.
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
        }
        // If it's just a simple predicate, its result is already on the stack from
        // exitPredicate, so we do nothing.
    }

    @Override
    public void enterPredicate(QueryParser.PredicateContext ctx) {
        log.info("    -> Entering Predicate: '{}'", ctx.getText());
    }

    @Override
    public void exitPredicate(QueryParser.PredicateContext ctx) {
        // todo cleanup and check for empty predicates
        log.info("    <- Exiting Predicate: '{}'", ctx.getText());

        String field = mapIdentifierToEntityField(ctx.IDENTIFIER().getText());
        String operator = getOperator(ctx);

        // This is a leaf node in the expression tree, so we process it and push its
        // string representation to the stack.
        String rawValue = ctx.value().getText();
        Object value = rawValue.substring(1, rawValue.length() - 1); // Remove quotes

        if ("status".equalsIgnoreCase(field)) {
            value = org.jboss.sbomer.service.nextgen.core.enums.EventStatus.valueOf(value.toString());
        }

        if ("created".equalsIgnoreCase(field) ||
                "updated".equalsIgnoreCase(field) ||
                "finished".equalsIgnoreCase(field)) {
            value = java.time.Instant.parse(value.toString());
        }

        if (ctx.CONTAINS() != null) {
            value = "%" + value + "%";
        }

        String paramName = "param" + paramIndex++;
        parameters.put(paramName, value);

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

    private String mapIdentifierToEntityField(String identifier) {
        // For now, we assume a direct mapping. This can be expanded.
        return identifier;
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
}
