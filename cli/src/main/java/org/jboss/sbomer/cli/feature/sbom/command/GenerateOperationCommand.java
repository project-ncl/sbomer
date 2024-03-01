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

import java.nio.file.Path;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
        mixinStandardHelpOptions = true,
        name = "generate-operation",
        aliases = { "go" },
        description = "SBOM generation for an operation",
        subcommands = { CycloneDxGenerateOperationCommand.class })
public class GenerateOperationCommand {

    @Getter
    @Option(
            names = { "-c", "--config", },
            paramLabel = "FILE",
            description = "Location of the runtime configuration file.",
            required = true,
            scope = ScopeType.INHERIT)
    Path configPath;

    @Getter
    @Option(
            names = { "--index" },
            description = "Index to select the product configuration passed in the --config option. Starts from 0. If not provided SBOM will be generated for every product in the config serially.",
            scope = ScopeType.INHERIT)
    Integer index;

    @Getter
    @Option(
            names = { "-op", "--operation-id" },
            description = "Operation identifier to generate the SBOM for",
            required = true)
    String operationId;

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
            description = "The directory where the POM file should be created. Default: ${DEFAULT-VALUE}",
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
