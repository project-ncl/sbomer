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
package org.jboss.sbomer.feature.sbom.cli;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.jboss.sbomer.core.cli.SbomerTopCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        mixinStandardHelpOptions = true,
        name = "sbom",
        description = "SBOM generation",
        subcommands = { ConfigCommand.class })
public class SbomCommand implements Callable<Integer>, SbomerTopCommand {

    // @Spec
    // protected CommandSpec spec;

    @Inject
    CommandLine.IFactory factory;

    @Override
    public Integer call() throws Exception {
        return 0;
    }

    // @Override
    // public Integer call() throws Exception {
    // String subcommands = spec.commandLine()
    // .getSubcommands()
    // .values()
    // .stream()
    // .map(cl -> cl.getCommandName())
    // .distinct()
    // .collect(Collectors.joining(", "));

    // throw new ParameterException(
    // spec.commandLine(),
    // "No subcommand provided, one or more is required: " + subcommands);
    // }

}
