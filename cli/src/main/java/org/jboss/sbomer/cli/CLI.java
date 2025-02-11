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
package org.jboss.sbomer.cli;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import org.jboss.sbomer.cli.errors.SbomerExitCodeExceptionMapper;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import io.quarkus.arc.All;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Help;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.RunAll;
import picocli.CommandLine.ScopeType;

@Slf4j
@QuarkusMain
@CommandLine.Command(name = "sbomer", mixinStandardHelpOptions = true)
public class CLI implements QuarkusApplication {
    @Inject
    CommandLine.IFactory factory;

    @All
    @Inject
    List<FeatureTopCommand> featureCommands;

    @Getter
    @Option(names = { "-v", "--verbose" }, scope = ScopeType.INHERIT)
    boolean verbose = false;

    @Override
    @ActivateRequestContext
    public int run(String... args) {
        CommandLine commandLine = new CommandLine(this, factory).setExecutionExceptionHandler(new ExceptionHandler())
                .setExecutionStrategy(new RunOnlyCallable())
                .setCommandName("sbomerctl");

        featureCommands.forEach(cmd -> {
            log.debug("Registering '{}' subcommand", cmd.getClass().getName());
            commandLine.addSubcommand(cmd);
        });

        commandLine.setExitCodeExceptionMapper(new SbomerExitCodeExceptionMapper());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);

        return commandLine.execute(args);
    }

    public static class RunOnlyCallable extends RunAll {
        @Override
        protected List<Object> handle(ParseResult parseResult) throws ExecutionException {
            return returnResultOrExit(recursivelyExecuteUserObject(parseResult, new ArrayList<>()));
        }

        private void runIfCallable(CommandLine parsed, List<Object> result) {
            Object command = parsed.getCommand();

            if (command instanceof Callable) {
                try {
                    @SuppressWarnings("unchecked")
                    Callable<Integer> callable = (Callable<Integer>) command;
                    Integer executionResult = callable.call();
                    parsed.setExecutionResult(executionResult);

                    result.add(executionResult);
                } catch (ParameterException | ExecutionException ex) {
                    throw ex;
                } catch (Exception ex) {
                    throw new ExecutionException(parsed, "Error while calling command (" + parsed + "): " + ex, ex);
                }
            }
        }

        /**
         * Checks whether one of previously run commands failed.
         *
         * @param result A {@link List} of return values from a command.
         * @return {@code true} if one of previous commands failed, {@code false} otherwise.
         */
        private boolean isFailed(List<Object> result) {
            return result.stream().anyMatch(r -> r instanceof Integer res && res != 0);
        }

        private List<Object> recursivelyExecuteUserObject(ParseResult parseResult, List<Object> result)
                throws ExecutionException {
            // Check whether something that was run previous failed
            // If yes, do not run subsequent commands
            if (isFailed(result)) {
                return result;
            }

            CommandLine parsed = parseResult.commandSpec().commandLine();

            runIfCallable(parsed, result);

            for (ParseResult pr : parseResult.subcommands()) {
                recursivelyExecuteUserObject(pr, result);
            }
            return result;
        }

        @Override
        public List<Object> handleParseResult(List<CommandLine> parsedCommands, PrintStream out, Help.Ansi ansi) {
            if (CommandLine.printHelpIfRequested(parsedCommands, out, err(), ansi)) { // NOSONAR: Method is deprecated,
                                                                                      // but we need to use it
                return returnResultOrExit(Collections.emptyList());
            }

            List<Object> result = new ArrayList<>();

            for (CommandLine parsed : parsedCommands) {
                runIfCallable(parsed, result);

            }
            return returnResultOrExit(result);
        }
    }

}
