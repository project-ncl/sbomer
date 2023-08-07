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

import java.nio.file.Path;

import org.jboss.sbomer.cli.feature.sbom.generate.MavenDominoGenerator;
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

    @Option(
            names = { "--domino-dir" },
            description = "Directory where the Domino tool can be found. Default: ${DEFAULT-VALUE}",
            defaultValue = "${env:SBOMER_DOMINO_DIR}",
            converter = PathConverter.class)
    Path dominoDir;

    @Override
    protected GeneratorType generatorType() {
        return GeneratorType.MAVEN_DOMINO;
    }

    @Override
    public Path doGenerate(String buildCmdOptions) {
        MavenDominoGenerator generator = MavenDominoGenerator.builder()
                .withDominoDir(dominoDir)
                .withDominoVersion(toolVersion())
                .withSettingsXmlPath(settingsXmlPath)
                .build();

        log.info("Starting SBOM generation using Domino");

        Path outputFile = generator.run(parent.getWorkdir(), generatorArgs().split(" "));

        log.info("SBOM generation finished");

        return outputFile;
    }
}
