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
package org.jboss.sbomer.cli.commands;

import javax.inject.Inject;

import org.jboss.sbomer.cli.CLI;
import org.jboss.sbomer.core.enums.GeneratorImplementation;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

// TODO: Add paramaters and implement the generation
@Command(mixinStandardHelpOptions = true, name = "generate", aliases = { "g" }, description = "Generate SBOM")
public class GenerateCommand implements Runnable {

    public static enum OutputFormat {
        json
    }

    @Inject
    CLI cli;

    @Option(
            names = { "-g", "--generator" },
            description = "The generator that should be used, valid values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}.",
            defaultValue = "CYCLONEDX")
    @Getter
    GeneratorImplementation generator;

    @Option(
            names = { "-f", "--format" },
            description = "Output format, valid values: ${COMPLETION-CANDIDATES}, default: ${DEFAULT-VALUE}.",
            defaultValue = "json")
    @Getter
    OutputFormat format;

    @Option(
            names = { "-o", "--output" },
            defaultValue = "bom.json",
            paramLabel = "DIR",
            description = "Path to file where the SBOM should be generated")
    String output;

    @Override
    public void run() {

        cli.usage(this.getClass());
    }

}
