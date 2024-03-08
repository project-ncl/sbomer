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
package org.jboss.sbomer.cli.errors;

import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IExitCodeExceptionMapper;
import picocli.CommandLine.UnmatchedArgumentException;

/**
 * Takes care of providing appropriate {@code exitCode} so that the execution can be terminated with the correct one.
 */
public class SbomerExitCodeExceptionMapper implements IExitCodeExceptionMapper {

    /**
     * Returns {@code exitCode} for known exceptions or {@code 1} otherwise.
     */
    @Override
    public int getExitCode(Throwable exception) {
        if (exception instanceof CommandLineException) {
            return ((CommandLineException) exception).getExitCode();
        }

        if (exception instanceof UnmatchedArgumentException) {
            return ExitCode.USAGE;
        }

        return ExitCode.SOFTWARE;
    }

}
