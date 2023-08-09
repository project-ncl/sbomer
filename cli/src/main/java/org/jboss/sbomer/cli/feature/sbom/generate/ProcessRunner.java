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
package org.jboss.sbomer.cli.feature.sbom.generate;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Class used to manage {@link Process Processes}.
 *
 * @author Marek Goldmann
 */
@Slf4j
public class ProcessRunner {

    private ProcessRunner() {
        throw new IllegalStateException("This is a utility class that should not be instantiated");
    }

    /**
     * Perform validation of the Maven project directory located at a given path.
     *
     * @param workDir
     */
    private static void validateWorkDir(Path workDir) {
        log.debug("Validating working directory: '{}'...", workDir);

        if (workDir == null) {
            throw new ValidationException(
                    "Command execution validation failed",
                    Collections.singletonList("No working directory provided"));
        }

        if (!Files.exists(workDir)) {
            throw new ValidationException(
                    "Command execution validation failed",
                    Collections.singletonList(
                            String.format("Provided working directory '%s' does not exist", workDir.toString())));
        }

        if (!Files.isDirectory(workDir)) {
            throw new ValidationException(
                    "Command execution validation failed",
                    Collections.singletonList(
                            String.format("Provided working directory '%s' is not a directory", workDir.toString())));
        }

        log.debug("Working directory '{}' will be used", workDir);
    }

    /**
     * Run the {@link Process} for the
     *
     * @param workDir The {@link Path} to the working directory
     * @param command The command to execute
     * @throws ApplicationException in case the process cannot be started or failed.
     */
    public static void run(Path workDir, String... command) {
        if (Objects.isNull(command) || command.length == 0) {
            throw new ValidationException(
                    "Command execution validation failed",
                    Collections.singletonList(String.format("No command to provided")));
        }

        ProcessRunner.validateWorkDir(workDir);

        ProcessBuilder pb = new ProcessBuilder().redirectOutput(Redirect.INHERIT).redirectError(Redirect.INHERIT);

        pb.command(command);
        pb.directory(workDir.toAbsolutePath().toFile());

        log.info("Preparing to execute command: '{}'", pb.command().stream().collect(Collectors.joining(" ")));

        Process process = null;

        try {
            process = pb.start();
        } catch (IOException e) {
            throw new ApplicationException("Error while running the command", e);
        }

        int exitCode = -1;

        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            log.error("Unable to obtain the status for the process", e);
            Thread.currentThread().interrupt();
        }

        if (exitCode != 0) {
            throw new ApplicationException("Command failed, see logs above");
        }

        log.info("Command run successfully");
    }
}
