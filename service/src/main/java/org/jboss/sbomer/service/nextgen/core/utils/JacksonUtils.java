package org.jboss.sbomer.service.nextgen.core.utils;

import java.util.Iterator;
import java.util.Map;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JacksonUtils {
    private JacksonUtils() {
        throw new IllegalStateException("This is a utility class that should not be instantiated");
    }

    /**
     * Converts the {@link ObjectNode} into an object representation of a given class.
     *
     * @param clazz The class the object should be converted to
     * @param node The content that should be converted
     * @return Converted object.
     */
    public static <T> T parse(Class<T> clazz, ObjectNode node) {
        try {
            return ObjectMapperProvider.json().treeToValue(node, clazz);
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Unable to convert provided content ino a {} object", clazz.getName(), e);
        }
    }

    public static ObjectNode toObjectNode(Object payload) {
        try {
            return ObjectMapperProvider.json().convertValue(payload, ObjectNode.class);
        } catch (IllegalArgumentException e) {
            log.error("Failed to convert object: '{}' to JsonNode", payload, e);
            throw new ApplicationException("Failed to convert given object to JsonNode", e);
        }
    }

    /**
     * Merges two {@link JsonNode} objects that are in fact {@link ObjectNode}s.
     *
     * @param mainNode The main object.
     * @param updateNode The secondary object which should be merged with the main one.
     * @return Updated main object.
     */
    public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {
        if (mainNode.isObject() && updateNode.isObject()) {
            ObjectNode mainObjectNode = (ObjectNode) mainNode;
            Iterator<Map.Entry<String, JsonNode>> updateFields = updateNode.fields();

            while (updateFields.hasNext()) {
                Map.Entry<String, JsonNode> fieldEntry = updateFields.next();
                String fieldName = fieldEntry.getKey();
                JsonNode value = fieldEntry.getValue();

                if (mainObjectNode.has(fieldName) && mainObjectNode.get(fieldName).isObject() && value.isObject()) {
                    // Recursive merge for nested objects
                    merge(mainObjectNode.get(fieldName), value);
                } else {
                    // Overwrite or add the field
                    mainObjectNode.set(fieldName, value);
                }
            }
        }
        return mainNode;
    }
}
