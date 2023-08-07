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
package org.jboss.sbomer.cli.test.feature.sbom.generate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.jboss.sbomer.cli.feature.sbom.generate.MavenDominoGenerator;
import org.jboss.sbomer.cli.feature.sbom.generate.ProcessRunner;
import org.jboss.sbomer.core.errors.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class MavenDominoGeneratorTest {

    final Path dominoDir = Path.of("/path/to/domino/dir");
    final Path workDir = Path.of("work/dir");

    @Test
    void testFailedWhenNoDominoDirProvided(@TempDir Path projectDir) {
        ValidationException thrown = Assertions.assertThrows(ValidationException.class, () -> {
            MavenDominoGenerator.builder().withDominoDir(null).build().run(projectDir);
        });

        assertEquals("Domino validation failed", thrown.getLocalizedMessage());
        assertEquals(1, thrown.getErrors().size());
        assertEquals("No Domino directory provided", thrown.getErrors().get(0));
    }

    @Test
    void testFailedWhenDominoDirDoesntExist(@TempDir Path projectDir) {
        ValidationException thrown = Assertions.assertThrows(ValidationException.class, () -> {
            MavenDominoGenerator.builder().withDominoDir(Path.of("some/dir")).build().run(projectDir);
        });

        assertEquals("Domino validation failed", thrown.getLocalizedMessage());
        assertEquals(1, thrown.getErrors().size());
        assertEquals("Provided domino directory 'some/dir' doesn't exist", thrown.getErrors().get(0));
    }

    @Test
    void testFailedWhenDominoDoesntExistForDefaultVersion(@TempDir Path projectDir, @TempDir Path wrongDir) {
        ValidationException thrown = Assertions.assertThrows(ValidationException.class, () -> {
            MavenDominoGenerator.builder().withDominoDir(wrongDir).build().run(projectDir);
        });

        assertEquals("Domino validation failed", thrown.getLocalizedMessage());
        assertEquals(1, thrown.getErrors().size());
        assertEquals(
                String.format("Domino could not be found on path '%s/domino.jar'", wrongDir),
                thrown.getErrors().get(0));
    }

    @Test
    void testFailedWhenDominoDirDoesntExistWithCustomVersion(@TempDir Path projectDir, @TempDir Path dominoDir) {
        ValidationException thrown = Assertions.assertThrows(ValidationException.class, () -> {
            MavenDominoGenerator.builder().withDominoDir(dominoDir).withDominoVersion("1.2.3").build().run(projectDir);
        });

        assertEquals("Domino validation failed", thrown.getLocalizedMessage());
        assertEquals(1, thrown.getErrors().size());
        assertEquals(
                String.format("Domino could not be found on path '%s/domino-1.2.3.jar'", dominoDir),
                thrown.getErrors().get(0));
    }

    @Test
    void testGenerate() {
        MavenDominoGenerator generator = MavenDominoGenerator.builder().withDominoDir(dominoDir).build();

        List<String> cmd = generate(generator, dominoDir, workDir);

        assertEquals(
                Arrays.asList(
                        "java",
                        "-Xms256m",
                        "-Xmx512m",
                        "-Dquarkus.args=\"\"",
                        "-jar",
                        "/path/to/domino/dir/domino.jar",
                        "report",
                        "--project-dir=work/dir",
                        "--output-file=bom.json",
                        "--manifest"),
                cmd);
    }

    @Test
    void testGenerateWithSettingsXml() {
        var settingsXmlPath = Path.of("settings.xml");

        MavenDominoGenerator generator = MavenDominoGenerator.builder()
                .withDominoDir(dominoDir)
                .withSettingsXmlPath(settingsXmlPath)
                .build();

        List<String> cmd = generate(generator, dominoDir, workDir);

        assertEquals(
                Arrays.asList(
                        "java",
                        "-Xms256m",
                        "-Xmx512m",
                        "-Dquarkus.args=\"\"",
                        "-jar",
                        "/path/to/domino/dir/domino.jar",
                        "report",
                        "--project-dir=work/dir",
                        "--output-file=bom.json",
                        "--manifest",
                        "-s",
                        "settings.xml"),
                cmd);
    }

    @Test
    void testGenerateWithCustomArgs() {
        MavenDominoGenerator generator = MavenDominoGenerator.builder().withDominoDir(dominoDir).build();

        List<String> cmd = generate(generator, dominoDir, workDir, "one-arg", "1", "--test");

        assertEquals(
                Arrays.asList(
                        "java",
                        "-Xms256m",
                        "-Xmx512m",
                        "-Dquarkus.args=\"\"",
                        "-jar",
                        "/path/to/domino/dir/domino.jar",
                        "report",
                        "--project-dir=work/dir",
                        "--output-file=bom.json",
                        "--manifest",
                        "one-arg",
                        "1",
                        "--test"),
                cmd);
    }

    @Test
    void testGenerateWithEmptyCustomArgs() {
        MavenDominoGenerator generator = MavenDominoGenerator.builder().withDominoDir(dominoDir).build();

        List<String> cmd = generate(generator, dominoDir, workDir, "");

        assertEquals(
                Arrays.asList(
                        "java",
                        "-Xms256m",
                        "-Xmx512m",
                        "-Dquarkus.args=\"\"",
                        "-jar",
                        "/path/to/domino/dir/domino.jar",
                        "report",
                        "--project-dir=work/dir",
                        "--output-file=bom.json",
                        "--manifest"),
                cmd);
    }

    private List<String> generate(MavenDominoGenerator generator, Path dominoDir, Path workDir, String... args) {
        ArgumentCaptor<Path> workDirCaptor = ArgumentCaptor.forClass(Path.class);
        ArgumentCaptor<String[]> commandCaptor = ArgumentCaptor.forClass(String[].class);

        try (MockedStatic<ProcessRunner> runnerMock = Mockito.mockStatic(ProcessRunner.class)) {
            runnerMock.when(() -> ProcessRunner.run(workDirCaptor.capture(), commandCaptor.capture()))
                    .thenAnswer((Answer<Void>) invocation -> null);

            try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
                filesMock.when(() -> Files.exists(workDir)).thenReturn(true);
                filesMock.when(() -> Files.isDirectory(workDir)).thenReturn(true);

                filesMock.when(() -> Files.exists(dominoDir)).thenReturn(true);
                filesMock.when(() -> Files.exists(Path.of(dominoDir.toString(), "domino.jar"))).thenReturn(true);

                var outputPath = generator.run(workDir, args);

                assertEquals(Path.of("work/dir/bom.json"), outputPath);
            }

        }

        assertEquals(Path.of("work/dir"), workDirCaptor.getValue());

        return Arrays.asList(commandCaptor.getValue());
    }
}
