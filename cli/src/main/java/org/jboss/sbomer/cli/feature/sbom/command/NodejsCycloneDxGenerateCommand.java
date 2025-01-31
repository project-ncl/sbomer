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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.sbomer.cli.feature.sbom.generate.ProcessRunner;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "nodejs-cyclonedx",
        aliases = { "nodejs-cyclonedx-plugin" },
        description = "SBOM generation for Node.js NPM projects using the CycloneDX Nodejs plugin",
        subcommands = { ProcessCommand.class })
public class NodejsCycloneDxGenerateCommand extends AbstractNodejsGenerateCommand {

    private static final String BOM_FILE_NAME = "bom.json";

    @Override
    protected Path doGenerate(String buildCmdOptions) {
        log.info("Starting SBOM generation using the CycloneDX NPM plugin...");

        ProcessRunner.run(parent.getWorkdir(), command(buildCmdOptions));

        return Path.of(parent.getWorkdir().toAbsolutePath().toString(), BOM_FILE_NAME);
    }

    private String[] command(String buildCmdOptions) {
        List<String> cmd = new ArrayList<>();

        cmd.add("cyclonedx-npm");
        cmd.addAll(Arrays.asList("--output-format", "JSON", "--spec-version", "1.6", "--output-file", BOM_FILE_NAME));

        cmd.addAll(Arrays.asList(generatorArgs().split(" ")));

        return cmd.toArray(new String[0]);
    }

    @Override
    protected GeneratorType generatorType() {
        return GeneratorType.NODEJS_CYCLONEDX;
    }

}
