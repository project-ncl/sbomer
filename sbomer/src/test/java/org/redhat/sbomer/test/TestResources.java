package org.redhat.sbomer.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestResources {
    /**
     * Reads test resource file and returns it as a String.
     */
    public static String asString(String path) throws IOException {
        return Files.readString(Paths.get("src", "test", "resources", path));
    }

    /**
     * Reads test resource JSON file and returns it as a Map.
     */
    @SuppressWarnings("unchecked")
    public static Map<Object, Object> asMap(String path) throws IOException {
        return new ObjectMapper().readValue(asString(path), Map.class);
    }
}
