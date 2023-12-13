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
import java.util.stream.Collectors;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "nodejs-cyclonedx",
        aliases = { "nodejs-cyclonedx-plugin" },
        description = "SBOM generation for Node.js NPM projects using the CycloneDX Nodejs plugin",
        subcommands = { ProcessCommand.class })
public class NodejsCycloneDxGenerateCommand extends AbstractNodejsGenerateCommand {

    private static final String BOM_FILE_NAME = "bom.json";

    @Override
    protected Path doGenerate(String buildCmdOptions) {
        ProcessBuilder processBuilder = new ProcessBuilder().inheritIO();

        processBuilder.command().add("cyclonedx-npm");
        processBuilder.command().addAll(Arrays.asList("--output-format", "JSON", "--output-file", BOM_FILE_NAME));

        String args = generatorArgs();
        processBuilder.command().addAll(Arrays.asList(args.split(" ")));

        log.info("Working directory: '{}'", parent.getWorkdir());
        processBuilder.directory(parent.getWorkdir().toFile());

        log.info(
                "Starting SBOM generation using the CycloneDX NPM plugin with command: '{}' ...",
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

        Path sbomPath = Path.of(parent.getWorkdir().toAbsolutePath().toString(), BOM_FILE_NAME);

        return sbomPath;
    }

    @Override
    protected GeneratorType generatorType() {
        return GeneratorType.NODEJS_CYCLONEDX;
    }

}
