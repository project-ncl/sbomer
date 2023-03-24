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
package org.jboss.sbomer.cli;

import javax.inject.Inject;

import org.jboss.sbomer.cli.commands.GenerateCommand;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.ScopeType;

@QuarkusMain
@CommandLine.Command(name = "sbomer", mixinStandardHelpOptions = true, subcommands = { GenerateCommand.class })
public class CLI implements QuarkusApplication {
    @Inject
    CommandLine.IFactory factory;

    public static enum Output {
        plain, yaml, json
    }

    @Option(names = { "-v", "--verbose" }, scope = ScopeType.INHERIT)
    boolean verbose = false;

    @Option(
            names = { "--output", "-o" },
            description = "Output format, valid values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}.",
            defaultValue = "plain",
            scope = ScopeType.INHERIT)
    @Getter
    Output output;

    public void usage(Class<?> command) {
        new CommandLine(command, factory).usage(System.out);
    }

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory).setExecutionExceptionHandler(new ExceptionHandler()).execute(args);
    }

    public int run(Class<? extends Runnable> command, String... args) {
        return new CommandLine(command, factory).execute(args);
    }
}
