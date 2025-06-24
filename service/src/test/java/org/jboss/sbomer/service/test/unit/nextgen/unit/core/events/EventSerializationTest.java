package org.jboss.sbomer.service.test.unit.nextgen.unit.core.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.jboss.sbomer.service.nextgen.core.dto.api.GenerationRequest;
import org.jboss.sbomer.service.nextgen.core.dto.api.Generator;
import org.jboss.sbomer.service.nextgen.core.dto.api.Target;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.enums.GenerationStatus;
import org.jboss.sbomer.service.nextgen.core.events.GenerationStatusChangeEvent;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class EventSerializationTest {
    @Test
    void testSerializationOfGenerationStatusChange() throws JsonProcessingException {
        GenerationRecord generationRecord = new GenerationRecord(
                "G",
                Instant.now(),
                Instant.now(),
                null,
                JacksonUtils.toObjectNode(
                        new GenerationRequest(
                                new Generator("syft", "1.16.0", null),
                                new Target("CONTAINER_IMAGE", "quay.io/org/image1:tag"))),
                null,
                GenerationStatus.FINISHED,
                null,
                null);

        GenerationStatusChangeEvent event = new GenerationStatusChangeEvent(generationRecord);

        ObjectNode node = JacksonUtils.toObjectNode(event);

        assertEquals("generation.status.change", node.get("type").asText());
        assertTrue(node.get("generation").isObject());
    }
}
