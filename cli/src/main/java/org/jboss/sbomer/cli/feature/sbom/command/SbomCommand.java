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
package org.jboss.sbomer.cli.feature.sbom.command;

import jakarta.inject.Inject;

import org.jboss.sbomer.cli.FeatureTopCommand;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        mixinStandardHelpOptions = true,
        name = "sbom",
        aliases = { "s" },
        description = "SBOM generation",
        subcommands = { AutoCommand.class, GenerateCommand.class, GenerateOperationCommand.class })
public class SbomCommand implements FeatureTopCommand {

    @Spec
    protected CommandSpec spec;

    @Inject
    CommandLine.IFactory factory;
}
