package org.jboss.sbomer.eventing.core.test.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.eventing.core.event.requests.GenerationRequestEvent;
import org.jboss.sbomer.eventing.core.event.requests.zip.GenerationRequestZipV1Alpha1Event;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;

public class GenerationRequestEventTest {

    private ObjectMapper objectMapper = ObjectMapperProvider.json();

    @Test
    void shouldHandleUnknownType() throws IOException {

        InvalidTypeIdException thrown = assertThrows(
                InvalidTypeIdException.class,
                () -> objectMapper.readValue(
                        TestResources.asString("events/ce-generation-request-broken-type.json"),
                        GenerationRequestEvent.class),
                "Expected exception, but nothing was thrown!");

        assertTrue(
                thrown.getMessage()
                        .contains(
                                "Could not resolve type id 'org.jboss.sbomer.generation.request.notexisting' as a subtype of `org.jboss.sbomer.func.events.requests.GenerationRequestEvent`: known type ids = [org.jboss.sbomer.generation.request.zip.v1alpha1]"));
    }

    @Test
    void shouldHandleMissingType() throws IOException {

        InvalidTypeIdException thrown = assertThrows(
                InvalidTypeIdException.class,
                () -> objectMapper.readValue(
                        TestResources.asString("events/ce-generation-request-missing-type.json"),
                        GenerationRequestEvent.class),
                "Expected exception, but nothing was thrown!");

        assertTrue(
                thrown.getMessage()
                        .contains(
                                "Could not resolve subtype of [simple type, class org.jboss.sbomer.func.events.requests.GenerationRequestEvent]: missing type id property 'type'"));
    }

    @Nested
    class ZipGenerationTest {

        @ParameterizedTest
        @ValueSource(strings = { "ce-generation-request-zip-default.json", "ce-generation-request-zip-xl.json" })
        void shouldDeserializeZip(String fileName) throws IOException {

            GenerationRequestEvent generationRequestEvent = objectMapper.readValue(
                    TestResources.asString(String.format("events/%s", fileName)),
                    GenerationRequestEvent.class);

            assertInstanceOf(GenerationRequestZipV1Alpha1Event.class, generationRequestEvent);

            GenerationRequestZipV1Alpha1Event zipRequestEvent = (GenerationRequestZipV1Alpha1Event) generationRequestEvent;

            assertEquals("https://host.com/a/path/to/some.zip", zipRequestEvent.getSpec().getUrl());
        }

    }
}
