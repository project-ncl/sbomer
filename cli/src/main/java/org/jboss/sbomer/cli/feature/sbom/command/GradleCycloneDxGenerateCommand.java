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
package org.jboss.sbomer.cli.feature.sbom.command;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

import static org.jboss.sbomer.core.features.sbom.utils.commandline.maven.MavenCommandLineParser.SPLIT_BY_SPACE_HONORING_SINGLE_AND_DOUBLE_QUOTES;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "gradle-cyclonedx-plugin",
        aliases = { "gradle-cyclonedx" },
        description = "SBOM generation for Gradle projects using the CycloneDX Gradle plugin",
        subcommands = { ProcessCommand.class })
public class GradleCycloneDxGenerateCommand extends AbstractGradleGenerateCommand {

    @Override
    protected Path doGenerate(String buildCmdOptions) {
        ProcessBuilder processBuilder = new ProcessBuilder().inheritIO();

        // Making the plugin version configurable
        processBuilder.command()
                .add(String.format("PLUGIN_VERSION=%s", toolVersion()));

        List.of(buildCmdOptions.split(SPLIT_BY_SPACE_HONORING_SINGLE_AND_DOUBLE_QUOTES))
                .stream()
                .forEach(processBuilder.command()::add);
        processBuilder.command().add("cyclonedxBom");

        if (initScriptPath != null) {
            log.debug("Using provided Gradle init script file located at '{}'", initScriptPath);
            processBuilder.command().add("--init-script");
            processBuilder.command().add(initScriptPath.toString());
        }

        String args = generatorArgs();
        processBuilder.command().addAll(Arrays.asList(args.split(" ")));

        log.info("Working directory: '{}'", parent.getWorkdir());
        processBuilder.directory(parent.getWorkdir().toFile());

        log.info(
                "Starting SBOM generation using the CycloneDX Gradle plugin with command: '{}' ...",
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

        Path sbomPath = Path.of(parent.getWorkdir().toAbsolutePath().toString(), "build", "sbom", "bom.json");

        return sbomPath;
    }

    @Override
    protected GeneratorType generatorType() {
        return GeneratorType.GRADLE_CYCLONEDX;
    }

}
