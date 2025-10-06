package org.jboss.sbomer.service.test.unit.nextgen.unit.query;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import org.junit.jupiter.params.provider.CsvSource;
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
                Arguments.of("status:NEW", "status = :param0", Map.of("param0", EventStatus.NEW)),
                Arguments.of(
                        "status:PROCESSED created:>=2023-08-20",
                        "(status = :param0 AND created >= :param1)",
                        Map.of("param0", EventStatus.PROCESSED, "param1", Instant.parse("2023-08-20T00:00:00Z"))),
                Arguments.of(
                        "status:ERROR,NEW",
                        "status IN (:param0, :param1)",
                        Map.of("param0", EventStatus.ERROR, "param1", EventStatus.NEW)),
                Arguments.of(
                        "id:E0AAAAA status:NEW created:<=2026-12-31",
                        "(id = :param0 AND status = :param1 AND created <= :param2)",
                        Map.of(
                                "param0",
                                "E0AAAAA",
                                "param1",
                                EventStatus.NEW,
                                "param2",
                                Instant.parse("2026-12-31T00:00:00Z"))),
                Arguments.of("reason:~\"Some\"", "reason LIKE :param0", Map.of("param0", "%Some%")),
                Arguments.of(
                        "status:NEW id:~\"A\"",
                        "(status = :param0 AND id LIKE :param1)",
                        Map.of("param0", EventStatus.NEW, "param1", "%A%")),
                Arguments.of("-reason:~\"Some\"", "(NOT reason LIKE :param0)", Map.of("param0", "%Some%")),
                // Nested Field Queries
                Arguments.of(
                        "metadata.type:PROCESS",
                        "(FUNCTION('jsonb_extract_path_text', metadata, 'type') = :param0)",
                        Map.of("param0", "PROCESS")),
                Arguments.of(
                        "metadata.type:PROCESS,DONE",
                        "(FUNCTION('jsonb_extract_path_text', metadata, 'type') = :param0 OR FUNCTION('jsonb_extract_path_text', metadata, 'type') = :param1)",
                        Map.of("param0", "PROCESS", "param1", "DONE")));
    }

    @DisplayName("Should correctly process valid sorting queries")
    @ParameterizedTest
    @CsvSource({ "'sort:status', 'ORDER BY status ASC'", "'sort:reason:asc', 'ORDER BY reason ASC'",
            "'sort:id:desc', 'ORDER BY id DESC'", "'status:NEW sort:created', 'ORDER BY created ASC'",
            "'created:>=2024-01-01 sort:id:desc', 'ORDER BY id DESC'" })
    void testValidSortingQueries(String query, String expectedOrderBy) {
        EventsQueryListener listener = assertDoesNotThrow(
                () -> eventsQueryProcessor.process(query),
                "A valid sort query should not throw an exception");

        assertEquals(expectedOrderBy, listener.getJpqlOrderByClause());
    }

    @DisplayName("Should reject queries with grammar violations")
    @ParameterizedTest
    @ValueSource(strings = { "status NEW", "status :", "status !! PROCESSED" })
    void testGrammarViolations(String query) {
        ClientException ex = assertThrows(ClientException.class, () -> eventsQueryProcessor.process(query));
        assertTrue(ex.getMessage().contains("Invalid query"));
    }

    @DisplayName("Should reject queries with semantic violations")
    @ParameterizedTest
    @ValueSource(strings = { "id:>=some-id", "created:~2024" })
    void testSemanticViolations(String query) {
        ClientException ex = assertThrows(ClientException.class, () -> eventsQueryProcessor.process(query));
        assertTrue(ex.getMessage().contains("Invalid query"));
    }

    @DisplayName("Should reject queries with invalid value formats")
    @ParameterizedTest
    @ValueSource(strings = { "created:20-08-2025", "status:PENDING" })
    void testInvalidFormats(String query) {
        ClientException ex = assertThrows(ClientException.class, () -> eventsQueryProcessor.process(query));
        assertTrue(ex.getMessage().contains("Invalid query"));
    }

    @Test
    @DisplayName("Should reject query with an unknown field")
    void testUnknownField() {
        String query = "priority:high";
        ClientException ex = assertThrows(ClientException.class, () -> eventsQueryProcessor.process(query));
        assertTrue(ex.getMessage().contains("Invalid query"));
        assertTrue(
                ex.getErrors()
                        .stream()
                        .anyMatch(
                                e -> e.contains("Unknown field: 'priority'")
                                        && e.contains("please specify a subfield")));
    }

    @DisplayName("Should reject invalid sorting queries")
    @ParameterizedTest
    @ValueSource(strings = { "sort:status:up", "sort:", "sort:created:", "sort: created", "sort:nonexistent_field" })
    void testInvalidSortingQueries(String query) {
        assertThrows(
                Exception.class,
                () -> eventsQueryProcessor.process(query),
                "Query '" + query + "' should have thrown an exception");
    }

    @Test
    @DisplayName("Should reject malformed identifier with multiple equals")
    void testMalformedIdentifier() {
        String query = "status:==============NEW";
        assertThrows(ClientException.class, () -> eventsQueryProcessor.process(query));
    }

    @DisplayName("Should reject invalid nested queries")
    @ParameterizedTest
    @ValueSource(
            strings = { "metadata:somevalue", "metadata.type:>somevalue", "metadata.type:>=somevalue",
                    "sort:metadata.type", "metadata.type:~PROCESS" })
    void testInvalidNestedQueries(String query) {
        Exception ex = assertThrows(Exception.class, () -> eventsQueryProcessor.process(query));

        if (ex instanceof ClientException) {
            assertTrue(((ClientException) ex).getMessage().contains("Invalid query"));
        }
    }
}
