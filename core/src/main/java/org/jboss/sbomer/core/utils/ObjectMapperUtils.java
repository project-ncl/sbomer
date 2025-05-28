package org.jboss.sbomer.core.utils;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ObjectMapperUtils {
    private ObjectMapperUtils() {
        throw new IllegalStateException("This is a utility class that should not be instantiated");
    }

    public static JsonNode toJsonNode(Object payload) {
        try {
            return ObjectMapperProvider.json().valueToTree(payload);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert object: '{}' to JsonNode", payload, e);
            throw new ApplicationException("Failed to convert given object to JsonNode", e);
        }
    }
}
