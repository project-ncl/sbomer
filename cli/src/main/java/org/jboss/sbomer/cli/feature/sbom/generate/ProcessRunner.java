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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
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
                    Collections
                            .singletonList(String.format("Provided working directory '%s' does not exist", workDir)));
        }

        if (!Files.isDirectory(workDir)) {
            throw new ValidationException(
                    "Command execution validation failed",
                    Collections.singletonList(
                            String.format("Provided working directory '%s' is not a directory", workDir)));
        }

        log.debug("Working directory '{}' will be used", workDir);
    }

    /**
     * Executes the provided {@code command} in the {@code workDir}.
     *
     * @param workDir The {@link Path} to the working directory
     * @param command The command to execute
     * @throws ApplicationException in case the process cannot be started or failed.
     */
    public static void run(Path workDir, String... command) {
        run(Collections.emptyMap(), workDir, command);
    }

    /**
     * Executes the provided {@code command} in the {@code workDir}.
     *
     * @param environment A {@link Map} containing environment variables that should be added to the execution.
     * @param workDir The {@link Path} to the working directory
     * @param command The command to execute
     * @throws ApplicationException in case the process cannot be started or failed.
     */
    public static void run(Map<String, String> environment, Path workDir, String... command) {
        if (Objects.isNull(command) || command.length == 0) {
            throw new ValidationException(
                    "Command execution validation failed",
                    Collections.singletonList("No command to provided"));
        }

        ProcessRunner.validateWorkDir(workDir);

        ProcessBuilder pb = new ProcessBuilder();

        pb.environment().putAll(environment);

        // Handle stdout and stderr together. This means that we
        // do not distinguish between these two streams in launched commands.
        // We will log both to stdout.
        pb.redirectErrorStream(true);

        pb.command(command);
        log.info("Command to run: '{}'", pb.command().stream().map(Object::toString).collect(Collectors.joining(" ")));

        pb.directory(workDir.toAbsolutePath().toFile());
        log.info("Working directory: '{}'", pb.directory());

        log.info("Starting execution...");

        Process process = null;

        try {
            process = pb.start();
        } catch (IOException e) {
            throw new ApplicationException("Error while running the command", e);
        }

        log.info("Starting processing of output...");

        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info(line);
                }
            }
        } catch (IOException e) {
            log.error(
                    "An error ocurred while procesing the output of the command. This is not fatal and will be ignored.",
                    e);
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
