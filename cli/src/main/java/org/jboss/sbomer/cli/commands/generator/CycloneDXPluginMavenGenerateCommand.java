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
package org.jboss.sbomer.cli.commands.generator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.errors.ApplicationException;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "cyclonedx-plugin",
        aliases = { "c", "cdx", "cyclonedx" },
        description = "Generate SBOM using CycloneDX plugin.")
public class CycloneDXPluginMavenGenerateCommand extends AbstractMavenGenerateCommand {

    @Option(names = { "--plugin-version" }, description = "Version of the CycloneDX Maven plugin")
    String version = "2.7.5";

    @Override
    protected GeneratorImplementation getGeneratorType() {
        return GeneratorImplementation.CYCLONEDX;
    }

    @Override
    protected Path generate(String buildCmdOptions) {
        ProcessBuilder processBuilder = new ProcessBuilder().inheritIO();
        // Split the build command to be passed to the ProcessBuilder
        List.of(buildCmdOptions.split("\\s+")).stream().forEach(processBuilder.command()::add);
        processBuilder.command()
                .add(String.format("org.cyclonedx:cyclonedx-maven-plugin:%s:makeAggregateBom", version));
        processBuilder.command().add("-DoutputFormat=json");
        processBuilder.command().add("-DoutputName=bom");

        // This is ignored currently, see https://github.com/CycloneDX/cyclonedx-maven-plugin/pull/321
        // Leaving it here so we can decide what to do in the future
        // builder.command().add(String.format("-DoutputDirectory=%s", directory.toString()));

        if (parent.getSettingsXmlPath() != null) {
            log.debug(
                    "Using provided Maven settings.xml configuration file located at '{}'",
                    parent.getSettingsXmlPath());
            processBuilder.command().add("--settings");
            processBuilder.command().add(parent.getSettingsXmlPath().toString());
        }

        // Add default options
        // TODO: These are currently not configurable
        processBuilder.command().add("--batch-mode");

        if (!cli.isVerbose()) {
            processBuilder.command().add("--quiet");
            processBuilder.command().add("--no-transfer-progress");
        }

        log.info("Working directory: '{}'", parent.getParent().getTargetDir());
        processBuilder.directory(parent.getParent().getTargetDir().toFile());

        log.info(
                "Starting SBOM generation using the CycloneDX Maven plugin with command: '{}' ...",
                processBuilder.command().toString());
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

        Path sbomPath = Path.of(parent.getParent().getTargetDir().toAbsolutePath().toString(), "target", "bom.json");

        log.info("Generation finished, SBOM available at: '{}'", sbomPath);

        return sbomPath;
    }
}
