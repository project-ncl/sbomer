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

import static org.jboss.sbomer.core.features.sbom.utils.commandline.maven.MavenCommandLineParser.SPLIT_BY_SPACE_HONORING_SINGLE_AND_DOUBLE_QUOTES;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jboss.sbomer.cli.feature.sbom.generate.ProcessRunner;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "maven-cyclonedx-plugin",
        aliases = { "maven-cyclonedx" },
        description = "SBOM generation for Maven projects using the CycloneDX Maven plugin",
        subcommands = { ProcessCommand.class })
public class MavenCycloneDxGenerateCommand extends AbstractMavenGenerateCommand {

    @Override
    protected Path doGenerate(String buildCmdOptions) {
        log.info("Starting SBOM generation using the CycloneDX Maven plugin...");

        ProcessRunner.run(
                Map.of(
                        "MAVEN_OPTS",
                        "-XshowSettings:vm -XX:+PrintCommandLineFlags",
                        "JAVA_TOOL_OPTIONS",
                        "-XX:InitialRAMPercentage=75.0 -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"),
                parent.getWorkdir(),
                command(buildCmdOptions));

        return Path.of(parent.getWorkdir().toAbsolutePath().toString(), "target", "bom.json");
    }

    private String[] command(String buildCmdOptions) {

        List<String> cmd = new ArrayList<>(
                List.of(buildCmdOptions.split(SPLIT_BY_SPACE_HONORING_SINGLE_AND_DOUBLE_QUOTES)));
        cmd.add(String.format("org.cyclonedx:cyclonedx-maven-plugin:%s:makeAggregateBom", toolVersion()));
        cmd.add("-DoutputFormat=json");
        cmd.add("-DoutputName=bom");
        cmd.add("-DschemaVersion=1.6");

        if (settingsXmlPath != null) {
            log.debug("Using provided Maven settings.xml configuration file located at '{}'", settingsXmlPath);
            cmd.add("--settings");
            cmd.add(settingsXmlPath.toString());
        }

        cmd.addAll(Arrays.asList(generatorArgs().split(" ")));

        return cmd.toArray(new String[0]);
    }

    @Override
    protected GeneratorType generatorType() {
        return GeneratorType.MAVEN_CYCLONEDX;
    }

}
