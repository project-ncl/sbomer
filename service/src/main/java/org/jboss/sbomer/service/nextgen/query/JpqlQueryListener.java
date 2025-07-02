package org.jboss.sbomer.service.nextgen.query;

import org.jboss.sbomer.service.nextgen.antlr4.antlr.QueryBaseListener;
import org.jboss.sbomer.service.nextgen.antlr4.antlr.QueryParser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JpqlQueryListener extends QueryBaseListener {
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
    }

    @Override
    public void enterPredicate(QueryParser.PredicateContext ctx) {
        log.info("    -> Entering Predicate: '{}'", ctx.getText());
    }

    @Override
    public void exitPredicate(QueryParser.PredicateContext ctx) {
        log.info("    <- Exiting Predicate: '{}'", ctx.getText());
    }

    @Override
    public void enterValue(QueryParser.ValueContext ctx) {
        log.info("      -> Entering Value: '{}'", ctx.getText());
    }

    @Override
    public void exitValue(QueryParser.ValueContext ctx) {
        log.info("      <- Exiting Value: '{}'", ctx.getText());
    }

}
