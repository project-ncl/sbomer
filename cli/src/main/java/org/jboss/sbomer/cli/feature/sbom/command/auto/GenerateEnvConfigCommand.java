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
package org.jboss.sbomer.cli.feature.sbom.command.auto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.cli.feature.sbom.ConfigReader;
import org.jboss.sbomer.cli.feature.sbom.command.PathConverter;
import org.jboss.sbomer.cli.feature.sbom.service.PncService;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.EnvironmentAttributesUtils;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * <p>
 * Command to generate a runtime environment configuration file (compliant with SDKMan) for SBOMer generation
 * </p>
 */
@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "generate-env-config",
        description = "Generates the runtime environment configuration used for automation for a given PNC build")
public class GenerateEnvConfigCommand implements Callable<Integer> {

    public static enum ConfigFormat {
        yaml, json;
    }

    @Option(names = { "--build-id" }, required = true, description = "The PNC build identifier, example: AYHJRDPEUMYAC")
    String buildId;

    @Option(
            names = { "--format" },
            defaultValue = "yaml",
            description = "Format of the generated environment configuration.")
    ConfigFormat format;

    @Option(
            names = { "--target" },
            paramLabel = "FILE",
            description = "Location where the configuration environment file should be stored. If not provided, environment configuration will be printed to standard output.",
            converter = PathConverter.class)
    Path target;

    @Inject
    PncService pncService;

    @Inject
    ConfigReader configReader;

    /**
     * Retrieves environment configuration from a PNC Build, compliant with SDKMan.
     *
     * @return a Map object if the configuration could be retrieved or an empty map otherwise.
     */
    private Map<String, String> environmentConfig(Build build) {
        // Make sure there is no context
        MDCUtils.removeContext();
        MDCUtils.addBuildContext(build.getId());
        log.debug("Attempting to fetch environment configuration from a PNC build");

        Map<String, String> buildEnvAttributes = build.getEnvironment().getAttributes();
        if (buildEnvAttributes == null || buildEnvAttributes.isEmpty()) {
            log.debug("Build environment attributes not found for the specified PNC build");
            return Collections.emptyMap();
        }
        Map<String, String> envConfig = EnvironmentAttributesUtils.getSDKManCompliantAttributes(buildEnvAttributes);
        if (envConfig == null || envConfig.isEmpty()) {
            log.debug("No compliant SDKMan environment attributes could be generated");
            return Collections.emptyMap();
        }

        return envConfig;
    }

    /**
     *
     * @return {@code 0} in case the environment config file was generated successfully or {@code 1} in case the
     *         provided buildId could not be found. A returned empty Map is valid as default versions will be used
     */
    @Override
    public Integer call() throws Exception {

        Build build = pncService.getBuild(buildId);

        if (build == null) {
            log.warn("Could not retrieve PNC build '{}'", buildId);
            return GenerationResult.ERR_GENERAL.getCode();
        }

        Map<String, String> envConfig = environmentConfig(build);

        if (envConfig.isEmpty()) {
            log.debug(
                    "Could not obtain environment attributes for the '{}' build. The generation will use default versions!",
                    buildId);
        }

        log.debug("RAW envConfig: '{}'", envConfig);
        log.debug("Using {} format", format);

        String configuration;

        if (format.equals(ConfigFormat.json)) {

            configuration = configReader.getJsonObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(envConfig);
        } else {
            configuration = configReader.getYamlObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(envConfig);
        }

        if (target != null) {
            Files.writeString(target, configuration);
            log.info("Environment configuration saved as '{}' file", target.toAbsolutePath());
        } else {
            System.out.println(configuration);
        }

        return GenerationResult.SUCCESS.getCode();
    }
}
