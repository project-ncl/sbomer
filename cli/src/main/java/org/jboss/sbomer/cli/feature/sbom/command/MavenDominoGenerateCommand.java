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
package org.jboss.sbomer.cli.feature.sbom.command;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.jboss.sbomer.cli.feature.sbom.config.DefaultGenerationConfig.DefaultGeneratorConfig;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;

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

    private static final String BOM_FILE_NAME = "bom.json";

    @Option(
            names = { "--domino-dir" },
            description = "Directory where the Domino tool can be found. Default: ${DEFAULT-VALUE}",
            defaultValue = "${env:SBOMER_DOMINO_DIR}",
            converter = PathConverter.class)
    Path dominoDir;

    private Path getDominoPath() {
        if (dominoDir == null) {
            throw new ApplicationException(
                    "Directory containing the Domino tool was not provided, please use the --domino-dir option or set it via SBOMER_DOMINO_DIR environment variable");
        }

        Path dominoPath = dominoDir;
        String version = toolVersion();

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
    protected GeneratorType generatorType() {
        return GeneratorType.MAVEN_DOMINO;
    }

    @Override
    public Path doGenerate(String buildCmdOptions) {
        Path dominoPath = getDominoPath();

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
                String.format("--output-file=%s", BOM_FILE_NAME),
                "--manifest");

        log.info("Working directory: '{}'", parent.getWorkdir());
        processBuilder.directory(parent.getWorkdir().toFile());

        if (settingsXmlPath != null) {
            log.debug("Using provided Maven settings.xml configuration file located at '{}'", settingsXmlPath);
            processBuilder.command().add("-s");
            processBuilder.command().add(settingsXmlPath.toString());
        }

        String args = generatorArgs();
        processBuilder.command().addAll(Arrays.asList(args.split(" ")));

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

        return Path.of(getParent().getWorkdir().toAbsolutePath().toString(), BOM_FILE_NAME);
    }

}
