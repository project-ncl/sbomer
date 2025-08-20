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
 * Unless required by applicable law of a greed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.nextgen.query;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.jboss.sbomer.service.nextgen.antlr.QueryBaseListener;
import org.jboss.sbomer.service.nextgen.antlr.QueryParser;
import org.jboss.sbomer.service.nextgen.core.enums.EventStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventsQueryListener extends QueryBaseListener {

    private final Stack<String> queryParts = new Stack<>();
    private final Map<String, Object> parameters = new HashMap<>();
    private int paramIndex = 0;

    @Getter
    @AllArgsConstructor
    private static class ParsedValue {
        private String operator;
        private String stringValue;
    }

    public String getJpqlWhereClause() {
        if (queryParts.isEmpty()) {
            return "";
        }
        return queryParts.peek();
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public void exitQuery(QueryParser.QueryContext ctx) {
        if (ctx.term().size() > 1) {
            List<String> operands = new ArrayList<>();
            for (int i = 0; i < ctx.term().size(); i++) {
                operands.add(queryParts.pop());
            }
            Collections.reverse(operands);
            queryParts.push("(" + String.join(" AND ", operands) + ")");
        }
        log.trace("Final JPQL WHERE clause: '{}' with params: {}", getJpqlWhereClause(), parameters);
    }

    @Override
    public void exitTerm(QueryParser.TermContext ctx) {
        if (ctx.MINUS() != null) {
            String expr = queryParts.pop();
            queryParts.push("(NOT " + expr + ")");
        }
    }

    @Override
    public void exitAtom(QueryParser.AtomContext ctx) {
        if (ctx.value() != null) {
            throw new UnsupportedOperationException("Standalone search terms are not supported. Use key:value filters.");
        }

        String field = ctx.IDENTIFIER().getText();
        handleValueList(field, ctx.value_list());
    }

    private void handleValueList(String field, QueryParser.Value_listContext ctx) {
        List<ParsedValue> parsedValues = ctx.value().stream().map(this::parseValue).collect(Collectors.toList());

        if (parsedValues.size() == 1) {
            ParsedValue pv = parsedValues.get(0);
            validatePredicate(field, pv.getOperator());

            Object value = convertValue(field, pv.getStringValue());
            Object finalValue = pv.getOperator().equals("LIKE") ? "%" + value + "%" : value;

            String paramName = nextParamName();
            parameters.put(paramName, finalValue);
            queryParts.push(field + " " + pv.getOperator() + " :" + paramName);
            return;
        }

        // Multiple comma-separated values for LIKE is not supported
        if (parsedValues.get(0).getOperator().equals("LIKE")) {
            throw new UnsupportedOperationException("The 'LIKE' operator (~) cannot be used with comma-separated values.");
        }

        ParsedValue firstValue = parsedValues.get(0);
        String operator = firstValue.getOperator();

        if (!operator.equals("=") && !operator.equals("!=")) {
            throw new UnsupportedOperationException(
                    "Operator '" + operator + "' is not supported for comma-separated values in field '" + field + "'.");
        }

        List<String> paramNames = new ArrayList<>();
        for (ParsedValue pv : parsedValues) {
            validatePredicate(field, operator);
            Object finalValue = convertValue(field, pv.getStringValue());
            String paramName = nextParamName();
            parameters.put(paramName, finalValue);
            paramNames.add(":" + paramName);
        }

        String jpqlOperator = operator.equals("!=") ? "NOT IN" : "IN";
        queryParts.push(field + " " + jpqlOperator + " (" + String.join(", ", paramNames) + ")");
    }

    private ParsedValue parseValue(QueryParser.ValueContext vCtx) {
        String op = "=";
        if (vCtx.op != null) {
            if (vCtx.op.getType() == QueryParser.CONTAINS) {
                op = "LIKE";
            } else {
                op = vCtx.op.getText();
            }
        }
        String text = (vCtx.IDENTIFIER() != null) ? vCtx.IDENTIFIER().getText() : unquote(vCtx.STRING().getText());
        return new ParsedValue(op, text);
    }

    private Object convertValue(String field, String stringValue) {
        switch (field) {
            case "status":
                try {
                    return EventStatus.valueOf(stringValue.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid value for 'status'. Valid values: " + Arrays.toString(EventStatus.values()));
                }
            case "created":
            case "updated":
            case "finished":
                try {
                    return parseInstant(stringValue);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid timestamp format for '" + field + "'. " + e.getMessage());
                }
            case "id":
            case "reason":
                return stringValue;
            default:
                throw new IllegalArgumentException("Unknown field: '" + field + "'");
        }
    }

    private void validatePredicate(String field, String operator) {
        switch (field) {
            case "id":
            case "reason":
                if (!operator.equals("=") && !operator.equals("!=") && !operator.equals("LIKE")) {
                    throw new UnsupportedOperationException(
                            "Operator '" + operator + "' is not supported for string field '" + field + "'");
                }
                break;
            case "created":
            case "updated":
            case "finished":
                if (operator.equals("LIKE")) {
                    throw new UnsupportedOperationException(
                            "The 'LIKE' operator (~) is not supported for date field '" + field + "'");
                }
                break;
            case "status":
                if (!operator.equals("=") && !operator.equals("!=")) {
                    throw new UnsupportedOperationException(
                            "Only '=' and '!=' operators are supported for enum field '" + field + "'");
                }
                break;
        }
    }

    private String unquote(String s) {
        return (s == null || s.length() < 2) ? s : s.substring(1, s.length() - 1);
    }

    private String nextParamName() {
        return "param" + paramIndex++;
    }

    private Instant parseInstant(String stringValue) {
        try {
            return Instant.parse(stringValue);
        } catch (DateTimeParseException e) {
            // It's not in the standard Instant format
        }

        DateTimeFormatter customFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy")
                .optionalStart().appendPattern("-MM").optionalStart().appendPattern("-dd").optionalEnd().optionalEnd()
                .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1).parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0).toFormatter(Locale.ROOT)
                .withZone(java.time.ZoneOffset.UTC);

        try {
            return Instant.from(customFormatter.parse(stringValue));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Supported formats: 'yyyy', 'yyyy-MM', 'yyyy-MM-dd', or ISO-8601 ('2025-08-20T10:00:00Z').");
        }
    }
}
