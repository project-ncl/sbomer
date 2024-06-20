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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

public class ExceptionHandler implements IExecutionExceptionHandler {

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) throws Exception {
        CLI cli = (CLI) cmd.getCommandSpec().root().userObject();

        cmd.getErr().println();
        cmd.getErr().println(cmd.getColorScheme().errorText("🛑 Ooops, an error occurred!"));
        cmd.getErr().println();

        if (ex.getMessage() != null) {
            cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));
        }

        cmd.getErr().println();

        if (ex instanceof ClientException apiEx) {
            cmd.getErr().println(cmd.getColorScheme().errorText((apiEx.getErrorId())));
            cmd.getErr().println();

            if (apiEx.getErrors() != null) {
                apiEx.getErrors()
                        .forEach(
                                (String m) -> cmd.getErr()
                                        .println(cmd.getColorScheme().errorText(String.format("  * %s", m))));
            }
        }

        if (cli.verbose) {
            cmd.getErr().println();
            cmd.getErr().println("Stacktrace:");
            cmd.getErr().println();
            ex.printStackTrace(cmd.getErr());
        }

        int exitCode = cmd.getExitCodeExceptionMapper() != null ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                : cmd.getCommandSpec().exitCodeOnExecutionException();

        String failureReasonPath = System.getenv("SBOMER_CLI_FAILURE_REASON_PATH");

        if (failureReasonPath != null) {
            Files.write(
                    Path.of(failureReasonPath),
                    ObjectMapperProvider.json()
                            .writeValueAsString(Map.of("message", ex.getMessage(), "exitCode", exitCode))
                            .getBytes(StandardCharsets.UTF_8));
        }

        return exitCode;
    }

}
