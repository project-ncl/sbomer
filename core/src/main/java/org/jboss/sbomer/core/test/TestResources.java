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
package org.jboss.sbomer.core.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestResources {

    private TestResources() {
        // This is a utlity class and should not be instantiated
    }

    /**
     * Reads test resource file and returns it as a String.
     */
    public static String asString(Path path) throws IOException {
        return Files.readString(path);
    }

    /**
     * Reads test resource file and returns it as a String.
     */
    public static String asString(String path) throws IOException {
        return TestResources.asString(Paths.get("src", "test", "resources", path));
    }

    /**
     * Reads test resource JSON file and returns it as a Map.
     */
    @SuppressWarnings("unchecked")
    public static Map<Object, Object> asMap(String path) throws IOException {
        return new ObjectMapper().readValue(TestResources.asString(path), Map.class);
    }
}
