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
package org.jboss.sbomer.feature.sbom.cli.command;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.jboss.sbomer.core.enums.GeneratorType;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.feature.sbom.core.config.DefaultGenerationConfig.DefaultGeneratorConfig;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "maven-domino",
        description = "SBOM generation for Maven projects using the Domino tool",
        subcommands = { ProcessCommand.class },
        subcommandsRepeatable = true)
public class MavenDominoGenerateCommand extends AbstractMavenGenerateCommand {

    @Option(
            names = { "--domino-dir" },
            description = "Directory where the Domino tool can be found. Default: ${DEFAULT-VALUE}",
            converter = PathConverter.class)
    Path dominoDir;

    private Path getDominoPath() {
        if (dominoDir == null) {
            dominoDir = PathConverter.homeExpanded(System.getenv("DOMINO_DIR"));
        }

        if (dominoDir == null) {
            throw new ApplicationException(
                    "Directory containing the Domino tool was not provided, please use the --domino-dir option or set it via DOMINO_DIR environment variable");
        }

        Path dominoPath = dominoDir;
        String version = toolVersion(GeneratorType.MAVEN_DOMINO);

        if (version != null) {
            dominoPath = dominoPath.resolve(String.format("domino-%s.jar", version));
        } else {
            dominoPath = dominoPath.resolve("domino.jar");
        }

        if (!Files.exists(dominoPath)) {
            throw new ApplicationException("Domino could not be found on path '{}'", dominoPath.toAbsolutePath());
        }

        return dominoPath;

    }

    @Override
    public Path doGenerate() {
        Path dominoPath = getDominoPath();

        DefaultGeneratorConfig defaultGeneratorConfig = defaultGenerationConfig
                .forGenerator(GeneratorType.MAVEN_DOMINO);

        ProcessBuilder processBuilder = new ProcessBuilder().redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.INHERIT);

        processBuilder.command(
                "java",
                "-Xms256m",
                "-Xmx512m",
                // Workaround for Domino trying to parse what it shouldn't parse
                "-Dquarkus.args=\"\"",
                "-jar",
                dominoPath.toAbsolutePath().toString(),
                "from-maven",
                "report",
                String.format("--project-dir=%s", parent.getWorkdir().toAbsolutePath().toString()),
                String.format("--output-file=%s/bom.json", parent.getWorkdir().toAbsolutePath().toString()),
                "--manifest");

        log.info("Working directory: '{}'", parent.getWorkdir());
        processBuilder.directory(parent.getWorkdir().toFile());

        if (settingsXmlPath != null) {
            log.debug("Using provided Maven settings.xml configuration file located at '{}'", settingsXmlPath);
            processBuilder.command().add("-s");
            processBuilder.command().add(settingsXmlPath.toString());
        }

        if (generator.getArgs() == null) {
            String defaultArgs = defaultGeneratorConfig.defaultArgs();

            log.debug("Using default arguments for the Domino execution: {}", defaultArgs);
            processBuilder.command().addAll(Arrays.asList(defaultArgs.split(" ")));
        } else {
            log.debug("Using provided arguments for the Domino execution: {}", generator.getArgs());

            processBuilder.command().addAll(Arrays.asList(generator.getArgs().split(" ")));
        }

        log.info(
                "Starting SBOM generation using Domino '{}' with command: '{}'",
                dominoPath.toAbsolutePath(),
                processBuilder.command().stream().collect(Collectors.joining(" ")));

        Process process = null;

        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new ApplicationException("Error while running the command", e);
        }

        int exitCode = -1;

        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            throw new ApplicationException("Unable to obtain the status for the process", e);
        }

        if (exitCode != 0) {
            throw new ApplicationException("SBOM generation failed, see logs above");
        }

        Path sbomPath = Path.of(parent.getWorkdir().toAbsolutePath().toString(), "quarkus-bom-bom.json"); // TODO:
                                                                                                          // Hardcoded,
                                                                                                          // milestone
                                                                                                          // 1.
                                                                                                          // Domino's
                                                                                                          // --output-file
                                                                                                          // is
                                                                                                          // not
                                                                                                          // deterministic
                                                                                                          // now.
                                                                                                          // If
                                                                                                          // it
                                                                                                          // does
                                                                                                          // not
                                                                                                          // exist,
                                                                                                          // default
                                                                                                          // to
                                                                                                          // the
                                                                                                          // bom.json
                                                                                                          // file

        if (!Files.exists(sbomPath)) {
            sbomPath = Path.of(parent.getWorkdir().toAbsolutePath().toString(), "bom.json");
        }

        return sbomPath;
    }

}
