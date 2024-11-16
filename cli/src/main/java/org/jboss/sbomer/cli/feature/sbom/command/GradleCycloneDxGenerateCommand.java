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

import static org.jboss.sbomer.core.features.sbom.Constants.GRADLE_PLUGIN_VERSION_ENV_VARIABLE;
import static org.jboss.sbomer.core.features.sbom.utils.commandline.gradle.GradleCommandLineParser.extractGradleMainBuildCommand;
import static org.jboss.sbomer.core.features.sbom.utils.commandline.gradle.GradleCommandLineParser.extractGradleMajorVersion;
import static org.jboss.sbomer.core.features.sbom.utils.commandline.maven.MavenCommandLineParser.SPLIT_BY_SPACE_HONORING_SINGLE_AND_DOUBLE_QUOTES;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.sbomer.cli.feature.sbom.generate.ProcessRunner;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "gradle-cyclonedx-plugin",
        aliases = { "gradle-cyclonedx" },
        description = "SBOM generation for Gradle projects using the CycloneDX Gradle plugin",
        subcommands = { ProcessCommand.class })
public class GradleCycloneDxGenerateCommand extends AbstractGradleGenerateCommand {

    @Override
    protected Path doGenerate(String buildCmdOptions) {
        log.info("Starting SBOM generation using the CycloneDX Gradle plugin...");

        Map<String, String> environment = new HashMap<>();

        configureProcessEnvironmentVariable(buildCmdOptions, environment);

        environment.putAll(
                Map.of(
                        "GRADLE_OPTS",
                        "-XshowSettings:vm -XX:+PrintCommandLineFlags",
                        "JAVA_OPTS",
                        "-XX:InitialRAMPercentage=75.0 -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"));

        ProcessRunner.run(environment, parent.getWorkdir(), command(buildCmdOptions));

        return Path.of(parent.getWorkdir().toAbsolutePath().toString(), "build", "sbom", "bom.json");
    }

    private String[] command(String buildCmdOptions) {
        List<String> cmd = new ArrayList<>();

        configureProcessMainBuildCommands(buildCmdOptions, cmd);

        cmd.add("cyclonedxBom");
        cmd.add("--no-daemon");

        if (initScriptPath != null) {
            log.debug("Using provided Gradle init script file located at '{}'", initScriptPath);
            cmd.add("--init-script");
            cmd.add(initScriptPath.toString());
        }

        cmd.addAll(Arrays.asList(generatorArgs().split(" ")));

        return cmd.toArray(new String[cmd.size()]);
    }

    @Override
    protected GeneratorType generatorType() {
        return GeneratorType.GRADLE_CYCLONEDX;
    }

    private void configureProcessEnvironmentVariable(String buildCmdOptions, Map<String, String> environment) {
        // If there is an hint about the major Gradle version required, use it.
        Optional<Integer> gradleMajorVersion = extractGradleMajorVersion(buildCmdOptions);
        if (!gradleMajorVersion.isPresent() || gradleMajorVersion.get() >= 5) {
            environment.put(GRADLE_PLUGIN_VERSION_ENV_VARIABLE, toolVersion());
        } else {
            // If the version is previous 5, force the Gradle CycloneDX plugin version to 1.6.1 for backward
            // compatibility
            environment.put(GRADLE_PLUGIN_VERSION_ENV_VARIABLE, "1.6.1");
        }
    }

    private void configureProcessMainBuildCommands(String buildCmdOptions, List<String> cmd) {
        Optional<String> mainGradleBuildCommand = extractGradleMainBuildCommand(buildCmdOptions);
        if (!mainGradleBuildCommand.isPresent()) {
            throw new ApplicationException("Gradle build command is empty.");
        }

        List.of(mainGradleBuildCommand.get().split(SPLIT_BY_SPACE_HONORING_SINGLE_AND_DOUBLE_QUOTES))
                .stream()
                .forEach(cmd::add);
    }

}
