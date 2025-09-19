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
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.jboss.sbomer.service.nextgen.antlr.QueryBaseListener;
import org.jboss.sbomer.service.nextgen.antlr.QueryParser;
import org.jboss.sbomer.service.nextgen.core.enums.EventStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventsQueryListener extends QueryBaseListener {

    private static final Set<String> VALID_FIELDS = Set
            .of("id", "created", "updated", "finished", "metadata", "status", "reason");
    private static final Set<String> VALID_SORT_FIELDS = Set
            .of("id", "created", "updated", "finished", "status", "reason");
    private static final Set<String> VALID_NESTED_FIELDS = Set.of("metadata");
    private static final Set<String> STRING_FIELDS = Set.of("id", "reason");
    private static final Set<String> COMPARABLE_FIELDS = Set.of("created", "updated", "finished");

    private final Stack<String> queryParts = new Stack<>();
    private final Map<String, Object> parameters = new HashMap<>();
    private String orderByClause = null;
    private int paramIndex = 0;

    public String getJpqlWhereClause() {
        return queryParts.isEmpty() ? "" : queryParts.peek();
    }

    public String getJpqlOrderByClause() {
        return orderByClause != null ? orderByClause : "";
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public void exitQuery(QueryParser.QueryContext ctx) {
        long termCount = ctx.statement().stream().filter(stmt -> stmt.term() != null).count();

        if (termCount > 1) {
            List<String> operands = new ArrayList<>();
            for (int i = 0; i < termCount; i++) {
                operands.add(queryParts.pop());
            }
            Collections.reverse(operands);
            queryParts.push("(" + String.join(" AND ", operands) + ")");
        }
    }

    @Override
    public void exitSort(QueryParser.SortContext ctx) {
        String sortField = ctx.field.getText();
        // only one level of nesting is supported (e.g. metadata.key)
        String baseSortField = sortField.split("\\.", 2)[0];

        if (VALID_NESTED_FIELDS.contains(baseSortField) && sortField.contains(".")) {
            throw new IllegalArgumentException("Sorting by nested fields is not supported.");
        }

        if (!VALID_SORT_FIELDS.contains(sortField)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: '" + sortField + "'. Valid fields: " + String.join(", ", VALID_SORT_FIELDS));
        }

        String sortOrder = "ASC";
        if (ctx.direction != null && ctx.direction.getType() == QueryParser.DESC) {
            sortOrder = "DESC";
        }

        orderByClause = "ORDER BY " + sortField + " " + sortOrder;
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
        String field = ctx.qualified_field().getText();
        String baseField = field.split("\\.", 2)[0];

        if (VALID_NESTED_FIELDS.contains(field)) {
            throw new IllegalArgumentException(
                "The '" + field + "' field is a nested object and requires a subfield for querying (e.g., '" + field + ".subfield')."
            );
        }

        List<String> values = ctx.value_list()
                .value()
                .stream()
                .map(v -> v.WORD() != null ? v.WORD().getText() : unquote(v.STRING().getText()))
                .collect(Collectors.toList());

        String operator = (ctx.value_list().value(0).op == null) ? "=" : ctx.value_list().value(0).op.getText();

        if (VALID_NESTED_FIELDS.contains(baseField)) {
            handleNestedFieldSearch(baseField, field, values, operator);
        } else {
            handleStandardFieldSearch(field, values, operator);
        }
    }

    private void handleNestedFieldSearch(String baseField, String fullField, List<String> values, String operator) {
        if (!operator.equals("=")) {
            throw new UnsupportedOperationException("Only the equals (=) operator is supported for nested fields.");
        }

        String key = fullField.substring(baseField.length() + 1);
        List<String> orClauses = new ArrayList<>();

        for (String value : values) {
            String likePattern = "%\"" + key + "\":\"" + value + "\"%";
            String paramName = nextParamName();
            parameters.put(paramName, likePattern);
            orClauses.add(String.format("REPLACE(CAST(%s AS text), ' ', '') LIKE :%s", baseField, paramName));
        }
        queryParts.push("(" + String.join(" OR ", orClauses) + ")");
    }

    private void handleStandardFieldSearch(String field, List<String> values, String operator) {
        validateFieldAndOperator(field, operator);

        if (values.size() > 1) {
            if (operator.equals("~")) {
                throw new UnsupportedOperationException("LIKE ('~') operator cannot be used with multiple values.");
            }
            List<String> paramNames = new ArrayList<>();
            for (String value : values) {
                String paramName = nextParamName();
                parameters.put(paramName, convertValue(field, value));
                paramNames.add(":" + paramName);
            }
            queryParts.push(field + " IN (" + String.join(", ", paramNames) + ")");
        } else {
            Object paramValue = convertValue(field, values.get(0));
            if (operator.equals("~")) {
                operator = "LIKE";
                paramValue = "%" + paramValue + "%";
            }
            String paramName = nextParamName();
            parameters.put(paramName, paramValue);
            queryParts.push(field + " " + operator + " :" + paramName);
        }
    }

    private void validateFieldAndOperator(String field, String operator) {
        if (!VALID_FIELDS.contains(field)) {
            // **MODIFIED**: Improve the generic "Unknown field" error message.
            String nonNestedFields = VALID_FIELDS.stream()
                .filter(f -> !VALID_NESTED_FIELDS.contains(f))
                .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                "Unknown field: '" + field + "'. Valid fields are: " + nonNestedFields +
                ". For nested fields like 'metadata', please specify a subfield (e.g., 'metadata.key')."
            );
        }
        if (operator.equals("~") && !STRING_FIELDS.contains(field)) {
            throw new UnsupportedOperationException("LIKE ('~') operator is only for string fields: " + STRING_FIELDS);
        }
        if ((operator.equals(">") || operator.equals(">=") || operator.equals("<") || operator.equals("<="))
                && !COMPARABLE_FIELDS.contains(field)) {
            throw new UnsupportedOperationException(
                    "Comparison operators are only for date/number fields: " + COMPARABLE_FIELDS);
        }
    }

    private Object convertValue(String field, String stringValue) {
        switch (field) {
            case "status":
                try {
                    return EventStatus.valueOf(stringValue.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid status value. Valid values: " + Arrays.toString(EventStatus.values()));
                }
            case "created":
            case "updated":
            case "finished":
                return parseInstant(stringValue);
            default:
                return stringValue;
        }
    }

    private Instant parseInstant(String stringValue) {
        try {
            return Instant.parse(stringValue);
        } catch (DateTimeParseException e) {
            // Fallback for yyyy-MM-dd etc.
        }

        DateTimeFormatter customFormatter = new DateTimeFormatterBuilder().appendPattern("yyyy")
                .optionalStart()
                .appendPattern("-MM")
                .optionalStart()
                .appendPattern("-dd")
                .optionalEnd()
                .optionalEnd()
                .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
                .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .toFormatter(Locale.ROOT)
                .withZone(java.time.ZoneOffset.UTC);

        try {
            return Instant.from(customFormatter.parse(stringValue));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "Supported formats: 'yyyy', 'yyyy-MM', 'yyyy-MM-dd', or ISO-8601 ('2025-08-20T10:00:00Z').");
        }
    }

    private String unquote(String s) {
        return (s == null || s.length() < 2) ? s : s.substring(1, s.length() - 1);
    }

    private String nextParamName() {
        return "param" + paramIndex++;
    }
}
