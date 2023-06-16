/**
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
package org.jboss.sbomer.feature.sbom.core.test.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.feature.sbom.core.client.GitilesClient;
import org.jboss.sbomer.feature.sbom.core.config.ConfigReader;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonMappingException;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
// @WithKubernetesTestServer
public class ConfigReaderTest {

    @Inject
    ConfigReader configReader;

    @InjectMock
    @RestClient
    GitilesClient gitilesClient;

    Build build = Build.builder()
            .id("ARYT3LBXDVYAC")
            .scmUrl("https://code.engineering.redhat.com/gerrit/eclipse/microprofile-graphql.git")
            .scmTag("1.1.0.redhat-00008")
            .build();

    private byte[] getTestConfigAsBytes(String fileName) throws IOException {
        return Files.readString(Paths.get("src", "test", "resources", "sbomer-configs", fileName)).getBytes();
    }

    @Test
    void testConfigDoesNotExist() {
        Mockito.when(
                gitilesClient.fetchFile(
                        "eclipse/microprofile-graphql",
                        "refs/tags/1.1.0.redhat-00008",
                        ".sbomer/config.yaml"))
                .thenThrow(NotFoundException.class);

        assertNull(configReader.getConfig(build));
    }

    @Test
    void testConfigMultipleProducts() throws IOException {
        Mockito.when(
                gitilesClient.fetchFile(
                        "eclipse/microprofile-graphql",
                        "refs/tags/1.1.0.redhat-00008",
                        ".sbomer/config.yaml"))
                .thenReturn(new String(Base64.getEncoder().encode(getTestConfigAsBytes("multi-product.yaml"))));

        assertNotNull(configReader.getConfig(build));
    }

    @Test
    void testInvalidProcessorSlug() throws IOException {
        Mockito.when(
                gitilesClient.fetchFile(
                        "eclipse/microprofile-graphql",
                        "refs/tags/1.1.0.redhat-00008",
                        ".sbomer/config.yaml"))
                .thenReturn(
                        new String(
                                Base64.getEncoder().encode(getTestConfigAsBytes("invalid-wrong-processor-slug.yaml"))));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            configReader.getConfig(build);
        });

        assertEquals("Could not read configuration file", ex.getMessage());
        assertEquals(
                "Processor implementation: 'doesntexist' is not valid",
                ((JsonMappingException) ex.getCause()).getOriginalMessage());
    }

    @Test
    void testInvalidProcessorConfig() throws IOException {
        Mockito.when(
                gitilesClient.fetchFile(
                        "eclipse/microprofile-graphql",
                        "refs/tags/1.1.0.redhat-00008",
                        ".sbomer/config.yaml"))
                .thenReturn(
                        new String(
                                Base64.getEncoder().encode(getTestConfigAsBytes("invalid-array-instead-of-map.yaml"))));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            configReader.getConfig(build);
        });

        assertEquals("Could not read configuration file", ex.getMessage());
        assertEquals(
                "Invalid format of processors, a map expected",
                ((JsonMappingException) ex.getCause()).getOriginalMessage());
    }

    @Test
    void testInvalidConfig() throws IOException {
        Mockito.when(
                gitilesClient.fetchFile(
                        "eclipse/microprofile-graphql",
                        "refs/tags/1.1.0.redhat-00008",
                        ".sbomer/config.yaml"))
                .thenReturn(
                        new String(Base64.getEncoder().encode(getTestConfigAsBytes("invalid-processor-config.yaml"))));

        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            configReader.getConfig(build);
        });

        assertEquals("Could not read configuration file", ex.getMessage());
        assertTrue(((JsonMappingException) ex.getCause()).getMessage().startsWith("Unrecognized field \"dummy\""));
    }

    @Test
    void testOnlyErrataOverride() throws IOException {
        Mockito.when(
                gitilesClient.fetchFile(
                        "eclipse/microprofile-graphql",
                        "refs/tags/1.1.0.redhat-00008",
                        ".sbomer/config.yaml"))
                .thenReturn(
                        new String(Base64.getEncoder().encode(getTestConfigAsBytes("single-errata-override.yaml"))));

        assertNotNull(configReader.getConfig(build));
    }
}
