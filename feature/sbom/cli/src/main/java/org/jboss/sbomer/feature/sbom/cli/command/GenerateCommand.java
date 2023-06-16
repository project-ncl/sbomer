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

import java.nio.file.Path;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "generate",
        aliases = { "g" },
        description = "SBOM generation",
        subcommands = { MavenCycloneDxGenerateCommand.class, MavenDominoGenerateCommand.class })
public class GenerateCommand {

    @Getter
    @Option(names = { "-b", "--build-id" }, description = "Build identifier to generate the SBOM for", required = true)
    String buildId;

    @Getter
    @Option(
            names = { "-f", "--force" },
            description = "If the workdir directory should be cleaned up in case it already exists. Default: ${DEFAULT-VALUE}",
            scope = ScopeType.INHERIT)
    boolean force = false;

    @Getter
    @Option(
            names = { "--workdir" },
            defaultValue = "workdir",
            paramLabel = "DIR",
            description = "The directory where the source code should checked out. Default: ${DEFAULT-VALUE}",
            converter = PathConverter.class,
            scope = ScopeType.INHERIT)
    Path workdir;

    @Getter
    @Option(
            names = { "-o", "--output" },
            defaultValue = "bom.json",
            paramLabel = "FILE",
            description = "Output location where the generated BOM should be placed",
            converter = PathConverter.class,
            scope = ScopeType.INHERIT)
    Path output;
}
