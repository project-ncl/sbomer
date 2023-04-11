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
package org.jboss.sbomer.cli.commands.processor;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.jboss.sbomer.cli.CLI;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@Command(
        mixinStandardHelpOptions = true,
        name = "process",
        aliases = { "p" },
        description = "Process SBOM using selected processor",
        subcommands = { DefaultProcessCommand.class, PropertiesProcessCommand.class })
public class ProcessCommand implements Callable<Integer> {
    @Inject
    CLI cli;

    @Getter
    @Option(
            names = { "--sbom-id" },
            required = true,
            description = "The SBOM identifier to fetch the SBOM for processing.",
            scope = ScopeType.INHERIT)
    Long sbomId;

    @Spec
    CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        throw new ParameterException(spec.commandLine(), "Missing required subcommand");
    }
}
