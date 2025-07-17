package org.jboss.sbomer.service.test.unit.nextgen.unit.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.service.nextgen.core.enums.EventStatus;
import org.jboss.sbomer.service.nextgen.query.EventsQueryListener;
import org.jboss.sbomer.service.nextgen.query.EventsQueryProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class EventsQueryProcessorTest {

    private EventsQueryProcessor eventsQueryProcessor;

    @BeforeEach
    void setup() {
        eventsQueryProcessor = new EventsQueryProcessor();
    }

    @DisplayName("Should process valid queries successfully")
    @ParameterizedTest
    @MethodSource("provideValidQueries")
    void testValidQueries(String query, String expectedJpql, Map<String, Object> expectedParams) {
        EventsQueryListener listener = eventsQueryProcessor.process(query);

        assertNotNull(listener);
        assertEquals(expectedJpql, listener.getJpqlWhereClause());
        assertEquals(expectedParams, listener.getParameters());
    }

    private static Stream<Arguments> provideValidQueries() {
        return Stream.of(
                Arguments.of("id = \"E0AAAAA\"", "id = :param0", Map.of("param0", "E0AAAAA")),
                Arguments.of("status = \"NEW\"", "status = :param0", Map.of("param0", EventStatus.NEW)),
                Arguments.of("status != \"ERROR\"", "status != :param0", Map.of("param0", EventStatus.ERROR)),
                Arguments.of("reason ~ \"processed\"", "reason LIKE :param0", Map.of("param0", "%processed%")),
                Arguments.of(
                        "created < \"2022-01-01T00:00:00Z\"",
                        "created < :param0",
                        Map.of("param0", Instant.parse("2022-01-01T00:00:00Z"))),
                Arguments.of(
                        "finished > \"2024-01-01T00:00:00Z\"",
                        "finished > :param0",
                        Map.of("param0", Instant.parse("2024-01-01T00:00:00Z"))),
                Arguments.of(
                        "status = \"NEW\" AND reason ~ \"Event\"",
                        "(status = :param0 AND reason LIKE :param1)",
                        Map.of("param0", EventStatus.NEW, "param1", "%Event%")),
                Arguments.of(
                        "status = \"PROCESSED\" OR id = \"E0BBBBB\"",
                        "(status = :param0 OR id = :param1)",
                        Map.of("param0", EventStatus.PROCESSED, "param1", "E0BBBBB")),
                Arguments.of(
                        "(id = \"E0AAAAA\" OR id = \"E0BBBBB\") AND status = \"NEW\"",
                        "((id = :param0 OR id = :param1) AND status = :param2)",
                        Map.of("param0", "E0AAAAA", "param1", "E0BBBBB", "param2", EventStatus.NEW)),
                Arguments.of(
                        "reason ~ \"something went wrong\"",
                        "reason LIKE :param0",
                        Map.of("param0", "%something went wrong%")),
                Arguments.of(
                        "status = \"ERROR\" AND reason ~ \"error\"",
                        "(status = :param0 AND reason LIKE :param1)",
                        Map.of("param0", EventStatus.ERROR, "param1", "%error%")),
                Arguments.of(
                        "reason ~ \"fail\" OR reason ~ \"error\"",
                        "(reason LIKE :param0 OR reason LIKE :param1)",
                        Map.of("param0", "%fail%", "param1", "%error%")),
                Arguments.of("status = \"PROCESSED\"", "status = :param0", Map.of("param0", EventStatus.PROCESSED)),
                Arguments.of(
                        "created = \"2023-10-25\"",
                        "created = :param0",
                        Map.of("param0", Instant.parse("2023-10-25T00:00:00Z"))),
                Arguments.of(
                        "updated = \"2023-10-25 14:30\"",
                        "updated = :param0",
                        Map.of("param0", Instant.parse("2023-10-25T14:30:00Z"))),
                Arguments.of(
                        "finished >= \"2023-10-25 14:30:15\"",
                        "finished >= :param0",
                        Map.of("param0", Instant.parse("2023-10-25T14:30:15Z"))));
    }

    @DisplayName("Should reject queries with grammar violations")
    @ParameterizedTest
    @ValueSource(strings = { "id = E0AAAAA", "(id = \"E0AAAAA\"", "status =", "status !! \"PROCESSED\"",
            "id = \"E0AAAAA\" AND", "\"PROCESSED\" = status", "id ! ! = \"some-id\"", "id !== \"some-id\"" })
    void testGrammarViolations(String query) {
        ClientException ex = assertThrows(ClientException.class, () -> eventsQueryProcessor.process(query));
        assertTrue(ex.getMessage().contains("Invalid query"));
    }

    @DisplayName("Should reject queries with semantic violations")
    @ParameterizedTest
    @ValueSource(strings = { "id > \"E0AAAAA\"", "created ~ \"2024\"" })
    void testSemanticViolations(String query) {
        ClientException ex = assertThrows(ClientException.class, () -> eventsQueryProcessor.process(query));
        assertTrue(ex.getMessage().contains("Invalid query"));
    }

    @DisplayName("Should reject queries with invalid value formats")
    @ParameterizedTest
    @ValueSource(
            strings = { "updated = \"10-25-2023\"", "status=\"INCORRECT\"", "created = \"2023/10/25\"",
                    "finished = \"25-10-2023 10:00\"", "updated = \"2023-10-25T10:00:00\"",
                    "created = \"2023-10-25 10:00:00 AM\"", "finished = \"Friday, 25 October 2023\"",
                    "updated = \"just text\"" })
    void testInvalidFormats(String query) {
        ClientException ex = assertThrows(ClientException.class, () -> eventsQueryProcessor.process(query));
        assertTrue(ex.getMessage().contains("Invalid query"));
    }

    @Test
    @DisplayName("Should reject query with an unknown field")
    void testUnknownField() {
        String query = "description = \"Some description\"";
        ClientException ex = assertThrows(ClientException.class, () -> eventsQueryProcessor.process(query));
        assertTrue(ex.getMessage().contains("Invalid query"));
        assertTrue(ex.getErrors().stream().anyMatch(e -> e.contains("Unknown field: 'description'")));
    }

    @DisplayName("Should correctly handle queries with varied whitespace")
    @ParameterizedTest
    @ValueSource(strings = { "status=\"NEW\"", "status = \"NEW\"", "status      =       \"NEW\"",
            "    id = \"some-id-123\"",
            "status=\"NEW\"       OR       reason ~ \"E\"", "(      status = \"NEW\"      )",
            "reason = \"This is a valid reason\"", "reason = \"It's a test case\"",
            "(id=\"event-abc\" AND reason=\"It's all processed\") OR status=\"NEW\"",
            "status       =       \"NEW\"" })
    void testWhitespaceHandling(String query) {

        assertNotNull(eventsQueryProcessor.process(query));
    }

    @DisplayName("Should reject queries with invalid whitespace")
    @ParameterizedTest
    @ValueSource(strings = { "id ! = \"some-id\"" })
    void testInvalidWhitespace(String query) {
        ClientException ex = assertThrows(ClientException.class, () -> eventsQueryProcessor.process(query));
        assertTrue(ex.getMessage().contains("Invalid query"));
    }
}
