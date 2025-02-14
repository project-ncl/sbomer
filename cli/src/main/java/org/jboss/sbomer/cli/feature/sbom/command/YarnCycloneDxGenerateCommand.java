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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.sbomer.cli.feature.sbom.generate.ProcessRunner;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.jboss.sbomer.core.features.sbom.utils.FileUtils;
import org.jboss.sbomer.core.features.sbom.utils.commandline.maven.MavenCommandLineParser;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "yarn-cyclonedx",
        aliases = { "yarn-cyclonedx-plugin" },
        description = "SBOM generation for Node.js Yarn projects using the CycloneDX Nodejs plugin",
        subcommands = { ProcessCommand.class })
public class YarnCycloneDxGenerateCommand extends AbstractNodejsGenerateCommand {

    @Override
    protected Path doGenerate(String buildCmdOptions) {
        log.info("Starting SBOM generation using the CycloneDX Yarn plugin...");

        ProcessRunner.run(parent.getWorkdir(), command(buildCmdOptions));

        try {
            List<Path> path = FileUtils.findManifests(parent.getWorkdir());
            if (path.size() != 1) {
                throw new ApplicationException(
                        "Unable to find the generated SBOM under the '{}' directory",
                        parent.getWorkdir().toAbsolutePath());
            }
            return path.get(0);
        } catch (IOException e) {
            throw new ApplicationException(
                    "Unable to find the generated SBOM under the '{}' directory",
                    parent.getWorkdir().toAbsolutePath());
        }
    }

    private String[] command(String buildCmdOptions) {
        List<String> cmd = new ArrayList<>();
        cmd.addAll(Arrays.asList("yarn", "dlx", "-q"));
        cmd.add(String.format("@cyclonedx/yarn-plugin-cyclonedx@%s", toolVersion()));
        cmd.addAll(Arrays.asList("--output-file", FileUtils.MANIFEST_FILENAME));
        cmd.addAll(Arrays.asList(generatorArgs().split(" ")));

        return cmd.toArray(new String[0]);
    }

    @Override
    protected GeneratorType generatorType() {
        return GeneratorType.YARN_CYCLONEDX;
    }

}
