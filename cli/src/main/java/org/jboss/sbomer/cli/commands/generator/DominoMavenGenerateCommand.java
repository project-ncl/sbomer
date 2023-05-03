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
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.inject.Inject;

import org.jboss.sbomer.cli.CLI;
import org.jboss.sbomer.cli.converters.PathConverter;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.errors.ApplicationException;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "domino",
        aliases = { "d" },
        description = "Generate SBOM using Domino.")
public class DominoMavenGenerateCommand extends AbstractMavenGenerateCommand {

    @Inject
    CLI cli;

    @Option(
            names = { "--domino-version" },
            description = "Version of the Domino tool. It is assumed that the Domino jar is available under following name: 'domino-[VERSION].jar' on the path specified by the --domino-dir option. If version is not specified, the 'domino.jar' will be assumed.")
    String dominoVersion;

    @Option(
            names = { "--domino-dir" },
            description = "Directory where the Domino tool can be found. Default: ${DEFAULT-VALUE}",
            converter = PathConverter.class)
    Path dominoDir = Path.of(System.getenv("HOME"));

    @Override
    protected GeneratorImplementation getGeneratorType() {
        return GeneratorImplementation.DOMINO;
    }

    @Override
    protected Path generate() {
        Path dominoPath = dominoDir;

        if (dominoVersion != null) {
            dominoPath = dominoPath.resolve(String.format("domino-%s.jar", dominoVersion));
        } else {
            dominoPath = dominoPath.resolve("domino.jar");
        }

        if (!Files.exists(dominoPath)) {
            throw new ApplicationException("Domino could not be found on path '{}'", dominoPath.toAbsolutePath());
        }

        log.info("Using Domino: '{}'", dominoPath.toAbsolutePath());

        ProcessBuilder processBuilder = new ProcessBuilder().redirectOutput(Redirect.INHERIT)
                .redirectError(Redirect.INHERIT);
        processBuilder.command(
                "java",
                "-jar",
                dominoPath.toAbsolutePath().toString(),
                "from-maven",
                "report",
                String.format("--project-dir=%s", parent.getParent().getTargetDir().toAbsolutePath().toString()),
                String.format(
                        "--output-file=%s/bom.json",
                        parent.getParent().getTargetDir().toAbsolutePath().toString()),
                "--manifest",
                "--include-non-managed",
                "--warn-on-missing-scm");

        log.debug(processBuilder.command().toString());

        if (parent.getSettingsXmlPath() != null) {
            log.debug(
                    "Using provided Maven settings.xml configuration file located at '{}'",
                    parent.getSettingsXmlPath());
            processBuilder.command().add("-s");
            processBuilder.command().add(parent.getSettingsXmlPath().toString());
        }

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

        Path sbomPath = Path.of(parent.getParent().getTargetDir().toAbsolutePath().toString(), "quarkus-bom-bom.json"); // TODO:
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
            sbomPath = Path.of(parent.getParent().getTargetDir().toAbsolutePath().toString(), "bom.json");
        }

        log.info("Generation finished, SBOM available at: '{}'", sbomPath);

        return sbomPath;
    }
}
