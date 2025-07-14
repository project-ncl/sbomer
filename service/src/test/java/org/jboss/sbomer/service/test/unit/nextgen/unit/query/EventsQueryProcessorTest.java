package org.jboss.sbomer.service.test.unit.nextgen.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.jboss.sbomer.service.nextgen.core.enums.EventStatus;
import org.jboss.sbomer.service.nextgen.query.EventsQueryListener;
import org.jboss.sbomer.service.nextgen.query.EventsQueryProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class EventsQueryProcessorTest {

    @InjectMocks
    EventsQueryProcessor queryProcessor;

    @BeforeEach
    void beforeEach() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldProperlyHandleLikeOperator() {
        String query = "reason ~ \"something went wrong\"";

        EventsQueryListener listener = queryProcessor.process(query);


        String jpqlWhereClause = listener.getJpqlWhereClause();
        Map<String, Object> parameters = listener.getParameters();

        assertNotNull(jpqlWhereClause);
        assertEquals("reason LIKE :param0", jpqlWhereClause);

        assertNotNull(parameters);
        assertEquals(1, parameters.size());
        assertTrue(parameters.containsKey("param0"));
        assertEquals("%something went wrong%", parameters.get("param0"));
    }

    @Test
    void shouldHandleCombinedAndAndLikeOperators() {
        String query = "status = \"ERROR\" AND reason ~ \"error\"";

        EventsQueryListener listener = queryProcessor.process(query);

        String jpqlWhereClause = listener.getJpqlWhereClause();
        Map<String, Object> parameters = listener.getParameters();

        assertEquals("(status = :param0 AND reason LIKE :param1)", jpqlWhereClause);
        assertEquals(2, parameters.size());
        assertEquals(EventStatus.ERROR, parameters.get("param0"));
        assertEquals("%error%", parameters.get("param1"));
    }

    @Test
    void shouldHandleCombinedOrAndLikeOperators() {
        String query = "reason ~ \"fail\" OR reason ~ \"error\"";

        EventsQueryListener listener = queryProcessor.process(query);

        String jpqlWhereClause = listener.getJpqlWhereClause();
        Map<String, Object> parameters = listener.getParameters();

        assertEquals("(reason LIKE :param0 OR reason LIKE :param1)", jpqlWhereClause);
        assertEquals(2, parameters.size());
        assertEquals("%fail%", parameters.get("param0"));
        assertEquals("%error%", parameters.get("param1"));
    }

    @Test
    void shouldProperlyHandleStatusEquality() {
        String query = "status = \"PROCESSED\"";

        EventsQueryListener listener = queryProcessor.process(query);

        String jpqlWhereClause = listener.getJpqlWhereClause();
        Map<String, Object> parameters = listener.getParameters();

        assertEquals("status = :param0", jpqlWhereClause);
        assertEquals(1, parameters.size());
        assertEquals(EventStatus.PROCESSED, parameters.get("param0"));
    }
}
