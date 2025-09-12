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

    // todo refactor, make more generic
    private static final Set<String> VALID_FIELDS = Set
            .of("id", "created", "updated", "finished", "metadata", "status", "reason");
    private static final Set<String> VALID_SORT_FIELDS = Set
            .of("id", "created", "updated", "finished", "status", "reason");
    private static final Set<String> STRING_FIELDS = Set.of("id", "reason", "metadata");
    private static final Set<String> COMPARABLE_FIELDS = Set.of("created", "updated", "finished");

    private final Stack<String> queryParts = new Stack<>();
    private final Map<String, Object> parameters = new HashMap<>();
    private String orderByClause = null;
    private int paramIndex = 0;

    // get full JPQL WHERE clause
    public String getJpqlWhereClause() {
        return queryParts.isEmpty() ? "" : queryParts.peek();
    }

    // get full JPQL ORDER BY clause
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
        String sortOrder = "ASC";

        if (ctx.direction != null && ctx.direction.getType() == QueryParser.DESC) {
            sortOrder = "DESC";
        }

        if (!isValidSortField(sortField)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: '" + sortField + "'. Valid fields: " + String.join(", ", VALID_SORT_FIELDS));
        }

        orderByClause = "ORDER BY " + sortField + " " + sortOrder;
        log.debug("Sort by {} {}", sortField, sortOrder);
    }

    // supported fields for sorting
    private boolean isValidSortField(String field) {
        return VALID_SORT_FIELDS.contains(field);
    }

    @Override
    public void exitTerm(QueryParser.TermContext ctx) {
        // adding negation when minus is present
        if (ctx.MINUS() != null) {
            String expr = queryParts.pop();
            queryParts.push("(NOT " + expr + ")");
        }
    }

    @Override
    public void exitAtom(QueryParser.AtomContext ctx) {
        String fieldPath = ctx.path().getText();
        handleValueList(fieldPath, ctx.value_list());
    }

    private void handleValueList(String field, QueryParser.Value_listContext ctx) {
        List<String> values = ctx.value()
                .stream()
                .map(v -> v.WORD() != null ? v.WORD().getText() : unquote(v.STRING().getText()))
                .collect(Collectors.toList());

        String operator = getOperator(ctx.value(0));

        if (values.size() == 1) {
            handleSingleValue(field, values.get(0), operator);
        } else {
            handleMultipleValues(field, values, operator);
        }
    }

    private String getOperator(QueryParser.ValueContext valueCtx) {
        if (valueCtx.op == null) {
            return "="; // default operator when none specified
        }
        return valueCtx.op.getType() == QueryParser.CONTAINS ? "LIKE" : valueCtx.op.getText();
    }

    private void handleSingleValue(String fieldPath, String value, String operator) {
        String baseField = getBaseField(fieldPath);

        if (!VALID_FIELDS.contains(baseField)) {
            throw new IllegalArgumentException("Unknown field: '" + fieldPath + "'. Valid fields: " + VALID_FIELDS);
        }
        // todo enable like search for metadata fields
        if (operator.equals("LIKE") && (baseField.equals("metadata") || fieldPath.startsWith("metadata."))) {
            throw new UnsupportedOperationException(
                "LIKE operator cannot be used with metadata or nested metadata fields."
            );
        }
        if (operator.equals("LIKE") && !STRING_FIELDS.contains(baseField)) {
            throw new UnsupportedOperationException(
                    "LIKE operator can only be used with string fields: " + STRING_FIELDS);
        }
        if ((operator.equals(">") || operator.equals(">=") || operator.equals("<") || operator.equals("<="))
                && !COMPARABLE_FIELDS.contains(baseField)) {
            throw new UnsupportedOperationException(
                    "Operator '" + operator + "' can only be used with date or number fields: " + COMPARABLE_FIELDS);
        }

        Object convertedValue = convertValue(baseField, value);

        if (operator.equals("LIKE")) {
            convertedValue = "%" + convertedValue + "%";
        }

        String paramName = nextParamName();
        parameters.put(paramName, convertedValue);
        // If metadata is stored as JSON/JsonNode in the entity, cast to text for string
        // operations
        String fieldExpr = fieldExpression(fieldPath);
        queryParts.push(fieldExpr + " " + operator + " :" + paramName);
    }

    private void handleMultipleValues(String fieldPath, List<String> values, String operator) {
        String baseField = getBaseField(fieldPath);

        if (!VALID_FIELDS.contains(baseField)) {
            throw new IllegalArgumentException("Unknown field: '" + baseField + "'. Valid fields: " + VALID_FIELDS);
        }
        if (operator.equals("LIKE")) {
            throw new UnsupportedOperationException("LIKE operator cannot be used with multiple values");
        }
        if ((operator.equals(">") || operator.equals(">=") || operator.equals("<") || operator.equals("<="))
                && !COMPARABLE_FIELDS.contains(baseField)) {
            throw new UnsupportedOperationException(
                    "Operator '" + operator + "' can only be used with date or number fields: " + COMPARABLE_FIELDS);
        }

        List<String> paramNames = new ArrayList<>();
        for (String value : values) {
            String paramName = nextParamName();
            parameters.put(paramName, convertValue(baseField, value));
            paramNames.add(":" + paramName);
        }

        String fieldExpr = fieldExpression(baseField);
        queryParts.push(fieldExpr + " IN (" + String.join(", ", paramNames) + ")");
    }

    // Return an expression to use for the field in JPQL. For JSON fields (metadata)
    // cast to text so
    // string operators like LIKE work with Hibernate/JPA.
    private String fieldExpression(String fieldPath) {
        if (fieldPath.startsWith("metadata.")) {
            // This syntax is for PostgreSQL JSONB.
            // Example: metadata.components.format -> jsonb_extract_path_text(metadata,
            // 'components', 'format')
            String[] parts = fieldPath.split("\\.");
            String baseField = parts[0];
            String jsonPath = Arrays.stream(parts, 1, parts.length) // Get all parts after 'metadata'
                    .map(p -> "'" + p + "'") // Quote each part
                    .collect(Collectors.joining(", ")); // Join with commas

            return "jsonb_extract_path_text(" + baseField + ", " + jsonPath + ")";
        }
        return fieldPath; // No change for other fields
    }

    private String getBaseField(String fieldPath) {
        return fieldPath.split("\\.")[0];
    }

    // parsing field + values
    private Object convertValue(String field, String stringValue) {
        switch (field) {
            // parsing enum fields
            case "status":
                try {
                    return EventStatus.valueOf(stringValue.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Invalid status value. Valid values: " + Arrays.toString(EventStatus.values()));
                }
                // parsing date fields
            case "created":
            case "updated":
            case "finished":
                return parseInstant(stringValue);
            default:
                // other fields are string
                return stringValue;
        }
    }

    // parse date formats
    private Instant parseInstant(String stringValue) {
        try {
            return Instant.parse(stringValue);
        } catch (DateTimeParseException e) {
            // It's not in the standard Instant format
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

    // keeping count of params
    private String nextParamName() {
        return "param" + paramIndex++;
    }
}
