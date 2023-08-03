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
package org.jboss.sbomer.cli.feature.sbom.generate;

import java.io.IOException;
import java.util.stream.Collectors;

import org.jboss.sbomer.core.errors.ApplicationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Class used to manage {@link Process Processes}.
 *
 * @author Marek Goldmann
 */
@Slf4j
public class ProcessRunner {
    /**
     * Run the {@link Process} for the
     *
     * @param pb The {@link ProcessBuilder} instance to start the process from.
     * @throws ApplicationException in case the process cannot be started or failed.
     */
    public static void run(ProcessBuilder pb) {
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
            throw new ApplicationException("Unable to obtain the status for the process", e);
        }

        if (exitCode != 0) {
            throw new ApplicationException("Command failed, see logs above");
        }

        log.info("Command run successfully");
    }
}
