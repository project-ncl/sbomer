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

import java.nio.file.Path;

import org.jboss.sbomer.cli.commands.AbstractCommand;
import org.jboss.sbomer.cli.converters.PathConverter;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.ScopeType;

@Command(
        mixinStandardHelpOptions = true,
        name = "maven",
        aliases = { "m" },
        description = "Generate SBOM for a Maven project",
        subcommands = { CycloneDXPluginMavenGenerateCommand.class, DominoMavenGenerateCommand.class })
public class MavenGenerateCommand extends AbstractCommand {
    @Getter
    @ParentCommand
    GenerateCommand parent;

    @Getter
    @Option(
            names = { "-s", "--settings" },
            description = "Path to Maven settings.xml file that should be used for this run instead of the default one",
            converter = PathConverter.class,
            scope = ScopeType.INHERIT)
    Path settingsXmlPath;

    @Override
    public Integer call() throws Exception {
        throw new ParameterException(spec.commandLine(), "Missing required subcommand");
    }

}
