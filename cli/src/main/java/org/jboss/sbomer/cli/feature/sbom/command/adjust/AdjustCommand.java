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
package org.jboss.sbomer.cli.feature.sbom.command.adjust;

import java.nio.file.Path;

import org.jboss.sbomer.cli.feature.sbom.command.PathConverter;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@Command(
        mixinStandardHelpOptions = true,
        name = "adjust",
        description = "Adjust previously generated manifest so that it can be processed by SBOMer",
        subcommands = { SyftImageAdjustCommand.class },
        subcommandsRepeatable = true)
public class AdjustCommand {

    @Getter
    @Option(
            names = { "-p", "--path" },
            required = true,
            paramLabel = "FILE",
            description = "Location of the manifest file which should be adjusted",
            converter = PathConverter.class,
            scope = ScopeType.INHERIT)
    Path path;

    @Getter
    @Option(
            names = { "-o", "--output" },
            defaultValue = "bom.json",
            paramLabel = "FILE",
            description = "Location of the adjusted manifest",
            converter = PathConverter.class,
            scope = ScopeType.INHERIT)
    Path outputPath;
}
