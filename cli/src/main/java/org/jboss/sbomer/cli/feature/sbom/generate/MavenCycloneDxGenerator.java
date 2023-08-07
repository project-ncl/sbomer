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
package org.jboss.sbomer.cli.feature.sbom.generate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jboss.sbomer.core.errors.ValidationException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of the {@link SbomGenerator} which uses the Maven CycloneDx plugin to generate SBOMs for a Maven
 * project.
 *
 * @see https://github.com/CycloneDX/cyclonedx-maven-plugin
 * @author Marek Goldmann
 */
@Slf4j
@Builder(setterPrefix = "with")
@NoArgsConstructor
@AllArgsConstructor
public class MavenCycloneDxGenerator implements SbomGenerator {

    private static final String BOM_FILE_NAME = "bom.json"; // TODO: allow for configuration

    /**
     * Path to the directory that contains the Domino tool.
     */
    Path dominoDir;
    /**
     * Optional version of the Domino tool. Without it, a version-less JAR will be used.
     */
    String dominoVersion;
    /**
     * Path to the Maven {@code settings.xml} file. Relative to {@link #workDir}.
     */
    Path settingsXmlPath;
    /**
     * Output file name. Relative to {@link #workDir}.
     */
    @Builder.Default
    Path outputFile = Path.of(BOM_FILE_NAME);

    /**
     * Perform validation of the Domino tool.
     *
     * @return The {@link Path} to the Domino tool.
     *
     */
    private Path dominoToolPath() {
        log.debug("Validating Domino tool...");

        if (dominoDir == null) {
            throw new ValidationException(
                    "Domino validation failed",
                    Collections.singletonList("No Domino directory provided"));
        }

        if (!Files.exists(dominoDir)) {
            throw new ValidationException(
                    "Domino validation failed",
                    Collections
                            .singletonList(String.format("Provided domino directory '%s' doesn't exist", dominoDir)));
        }

        Path dominoPath = dominoDir;

        if (dominoVersion != null) {
            dominoPath = dominoPath.resolve(String.format("domino-%s.jar", dominoVersion));
        } else {
            dominoPath = dominoPath.resolve("domino.jar");
        }

        if (!Files.exists(dominoPath)) {
            throw new ValidationException(
                    "Domino validation failed",
                    Collections.singletonList(String.format("Domino could not be found on path '%s'", dominoPath)));
        }

        log.debug("Domino tool is valid and available at '{}', will use it", dominoPath);

        return dominoPath;

    }

    @Override
    public Path run(Path workDir, String... generatorArgs) {
        log.info("Preparing to generate SBOM using the Maven CycloneDx generator");

        Path dominoToolPath = dominoToolPath();

        String[] command = command(dominoToolPath, workDir, generatorArgs);

        ProcessRunner.run(workDir, command);

        return outputFile;
    }

    private String[] command(Path dominoToolPath, Path workDir, String... args) {
        List<String> cmd = new ArrayList<>();

        cmd.addAll(
                Arrays.asList(
                        "java",
                        "-Xms256m",
                        "-Xmx512m",
                        // Workaround for Domino trying to parse what it shouldn't parse
                        "-Dquarkus.args=\"\"",
                        "-jar",
                        dominoToolPath.toString(),
                        "report",
                        String.format("--project-dir=%s", workDir.toString()),
                        String.format("--output-file=%s", outputFile),
                        "--manifest"));

        if (args != null && args.length > 0) {
            log.debug("Validating additional arguments: '{}' (size: {})", Arrays.toString(args), args.length);

            for (String arg : args) {
                if (arg == null || arg.isBlank()) {
                    log.warn("Skipping adding empty argument to the command!");
                    continue;
                }

                cmd.add(arg);
            }
        } else {
            log.debug("No additional arguments provided");
        }

        if (settingsXmlPath != null) {
            log.debug("Using provided Maven settings.xml configuration file located at '{}'", settingsXmlPath);
            cmd.add("-s");
            cmd.add(settingsXmlPath.toString());
        }

        return cmd.toArray(new String[cmd.size()]);
    }
}
