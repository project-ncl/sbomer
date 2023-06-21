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
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.jboss.sbomer.cli.feature.sbom.config.DefaultGenerationConfig.DefaultGeneratorConfig;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "maven-cyclonedx-plugin",
        aliases = { "maven-cyclonedx" },
        description = "SBOM generation for Maven projects using the CycloneDX Maven plugin",
        subcommands = { ProcessCommand.class })
public class MavenCycloneDxGenerateCommand extends AbstractMavenGenerateCommand {

    @Override
    protected Path doGenerate() {
        DefaultGeneratorConfig defaultGeneratorConfig = defaultGenerationConfig
                .forGenerator(GeneratorType.MAVEN_CYCLONEDX);

        ProcessBuilder processBuilder = new ProcessBuilder().inheritIO();

        processBuilder.command(
                "mvn",
                String.format(
                        "org.cyclonedx:cyclonedx-maven-plugin:%s:makeAggregateBom",
                        toolVersion(GeneratorType.MAVEN_CYCLONEDX)),
                "-DoutputFormat=json",
                "-DoutputName=bom");

        // This is ignored currently, see https://github.com/CycloneDX/cyclonedx-maven-plugin/pull/321
        // Leaving it here so we can decide what to do in the future
        // builder.command().add(String.format("-DoutputDirectory=%s", directory.toString()));

        if (settingsXmlPath != null) {
            log.debug("Using provided Maven settings.xml configuration file located at '{}'", settingsXmlPath);
            processBuilder.command().add("--settings");
            processBuilder.command().add(settingsXmlPath.toString());
        }

        if (generator.getArgs() == null) {
            String defaultArgs = defaultGeneratorConfig.defaultArgs();

            log.debug("Using default arguments for the Maven CycloneDX plugin execution: {}", defaultArgs);
            processBuilder.command().addAll(Arrays.asList(defaultArgs.split(" ")));
        } else {
            log.debug("Using provided arguments for the Maven CycloneDX plugin execution: {}", generator.getArgs());

            processBuilder.command().addAll(Arrays.asList(generator.getArgs().split(" ")));
        }

        log.info("Working directory: '{}'", parent.getWorkdir());
        processBuilder.directory(parent.getWorkdir().toFile());

        log.info(
                "Starting SBOM generation using the CycloneDX Maven plugin with command: '{}' ...",
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

        Path sbomPath = Path.of(parent.getWorkdir().toAbsolutePath().toString(), "target", "bom.json");

        return sbomPath;
    }

}
