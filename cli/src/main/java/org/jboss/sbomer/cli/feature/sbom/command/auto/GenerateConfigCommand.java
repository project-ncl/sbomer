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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.sbomer.cli.errors.pnc.MissingPncBuildException;
import org.jboss.sbomer.cli.feature.sbom.ConfigReader;
import org.jboss.sbomer.cli.feature.sbom.ProductVersionMapper;
import org.jboss.sbomer.cli.feature.sbom.command.PathConverter;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.config.ConfigSchemaValidator;
import org.jboss.sbomer.core.config.DefaultGenerationConfig.DefaultGeneratorConfig;
import org.jboss.sbomer.core.config.SbomerConfigProvider;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.DefaultProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.jboss.sbomer.core.features.sbom.utils.EnvironmentAttributesUtils;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.pnc.PncService;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * <p>
 * Command to generate a runtime configuration file for SBOMer generation
 * </p>
 */
@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "generate-config",
        description = "Generates the runtime configuration used for automation for a given PNC build")
public class GenerateConfigCommand implements Callable<Integer> {

    public enum ConfigFormat {
        YAML, JSON;
    }

    @Option(
            names = { "-c", "--config", },
            paramLabel = "FILE",
            description = "Path to a configuration file in JSON or YAML format. If provided, the configuration file will be read, extended (if needed) and validated.")
    Path configPath;

    @Option(
            names = { "--build-id" },
            required = true,
            description = "The PNC build identifier, example: AYHJRDPEUMYAC.")
    String buildId;

    @Option(names = { "--format" }, defaultValue = "yaml", description = "Format of the generated configuration.")
    ConfigFormat format;

    @Option(
            names = { "--target" },
            paramLabel = "FILE",
            description = "Location where the configuration file should be stored. If not provided, configuration will be printed to standard output.",
            converter = PathConverter.class)
    Path target;

    @Inject
    ConfigReader configReader;

    @Inject
    PncService pncService;

    @Inject
    ConfigSchemaValidator configSchemaValidator;

    SbomerConfigProvider configAdjuster = new SbomerConfigProvider();

    protected SbomerConfigProvider sbomerConfigProvider = SbomerConfigProvider.getInstance();

    @Inject
    ProductVersionMapper productVersionMapper;

    private PncBuildConfig productConfig(Build build) {
        log.info("Obtaining runtime configuration for build '{}'", build.getId());

        // 1. Find if we can obtain the configuration from the file, if not found
        // 2. Find if we can find configuration in the mapper
        // 3. Try to use defaults (if possible)

        PncBuildConfig config = clientConfig(build);

        // Client configuration found, use it!
        if (config != null) {
            return config;
        }

        config = mappingConfig(build);

        // Mapping configuration found, use it!
        if (config != null) {
            return config;
        }

        log.warn("Runtime configuration for build '{}' could not be found", build.getId());

        // Configuration file could not be found
        return null;
    }

    /**
     * Retrieves configuration from a SBOMer configuration file stored in the source code repository.
     *
     * @return {@link Config} object if the configuration could be retrieved or {@code null} otherwise.
     */
    private PncBuildConfig clientConfig(Build build) {
        log.debug("Attempting to fetch configuration from source code repository");

        PncBuildConfig config = (PncBuildConfig) configReader.getConfig(build);

        if (config == null) {
            log.debug("Configuration file not found in the source code repository");
            return null;
        }

        log.debug("Configuration file found in the source code repository");

        return config;
    }

    /**
     * Retrieves configuration from a SBOMer configuration file from the internal mapping.
     *
     * @return {@link Config} object if the configuration could be retrieved or {@code null} otherwise.
     */
    private PncBuildConfig mappingConfig(Build build) {
        log.debug("Attempting to fetch configuration from SBOMer internal mapping");

        List<ProductVersionRef> productVersions = pncService.getProductVersions(build.getId());

        if (productVersions.isEmpty()) {
            log.debug("Could not obtain PNC Product Version information for the '{}' PNC build", build.getId());
            return null;
        }

        List<PncBuildConfig> configs = new ArrayList<>();

        productVersions.forEach(productVersion -> {
            log.debug(
                    "Trying to find configuration in internal mapping for product version {}",
                    productVersion.getId());

            PncBuildConfig config = productVersionMapper.getMapping().get(productVersion.getId());

            if (config == null) {
                log.debug(
                        "Configuration not found in SBOMer internal mapping for product version: {}",
                        productVersion.getId());
                return;
            }

            log.debug("Configuration found in internal mapping for product version {}", productVersion.getId());

            configs.add(config);
        });

        if (configs.isEmpty()) {
            log.debug(
                    "No configuration found for product versions: {}",
                    productVersions.stream().map(pv -> pv.getId()).collect(Collectors.toList()));
            return null;
        }

        log.info(
                "Found {} configurations in the internal SBOMer mapping for build ID '{}'",
                configs.size(),
                build.getId());

        PncBuildConfig config = PncBuildConfig.builder()
                .withBuildId(build.getId())
                .withApiVersion("sbomer.jboss.org/v1alpha1")
                .withProducts(new ArrayList<>())
                .build();

        configs.forEach(cfg -> config.getProducts().addAll(cfg.getProducts()));

        log.info(
                "Found {} products in the internal SBOMer mapping for build ID '{}'",
                config.getProducts().size(),
                build.getId());

        return config;
    }

    /**
     * Retrieves environment configuration from a PNC Build, compliant with SDKMan.
     *
     * @return a Map object if the configuration could be retrieved or an empty map otherwise.
     * @author Andrea Vibelli
     */
    private Map<String, String> environmentConfig(Build build) {
        log.debug("Attempting to fetch environment configuration from a PNC build");

        Map<String, String> buildEnvAttributes = build.getEnvironment().getAttributes();
        if (buildEnvAttributes == null || buildEnvAttributes.isEmpty()) {
            log.debug("Build environment attributes not found for the specified PNC build");
            return Collections.emptyMap();
        }
        Map<String, String> envConfig = null;
        switch (build.getBuildConfigRevision().getBuildType()) {
            case GRADLE, MVN:
                envConfig = EnvironmentAttributesUtils.getSDKManCompliantAttributes(buildEnvAttributes);
                break;
            case NPM:
                envConfig = EnvironmentAttributesUtils.getNvmCompliantAttributes(buildEnvAttributes);
                break;
            default:
                break;
        }
        if (envConfig == null || envConfig.isEmpty()) {
            log.debug("No compliant SDKMan environment attributes could be generated");
            return Collections.emptyMap();
        }

        return envConfig;
    }

    /**
     * <p>
     * Returns the {@link GeneratorType} for a given {@link org.jboss.pnc.enums.BuildType}.
     * </p>
     *
     * <p>
     * When a the {@param buildType} cannot be converted, {@code null} is returned.
     * </p>
     *
     * @param buildType
     * @return {@link GeneratorType}
     */
    private static GeneratorType buildTypeToGeneratoType(org.jboss.pnc.enums.BuildType buildType) {
        if (buildType == null) {
            return null;
        }

        switch (buildType) {
            case GRADLE:
                return GeneratorType.GRADLE_CYCLONEDX;
            case MVN:
                return GeneratorType.MAVEN_CYCLONEDX;
            case NPM:
                return GeneratorType.NODEJS_CYCLONEDX;
            default:
                return null;
        }
    }

    /**
     * <p>
     * Prepares a default configuration ({@link PncBuildConfig}) for a given build identifier.
     * </p>
     *
     * <p>
     * Retrieves the build information, specifically the build type assigned to the build and based on it a
     * {@link IConfBuildConfigig} object is generated with default values.
     * </p>
     *
     * @return {@link PncBuildConfig} with default values
     */
    private PncBuildConfig initializeDefaultConfig(Build build) {
        GeneratorType generatorType = GenerateConfigCommand
                .buildTypeToGeneratoType(build.getBuildConfigRevision().getBuildType());

        if (generatorType == null) {
            log.warn("Unsupported build type: '{}'", build.getBuildConfigRevision().getBuildType());
            return null;
        }

        DefaultGeneratorConfig defaultGeneratorConfig = sbomerConfigProvider.getDefaultGenerationConfig()
                .forGenerator(generatorType);

        return PncBuildConfig.builder()
                .withBuildId(build.getId())
                .withProducts(
                        List.of(
                                ProductConfig.builder()
                                        .withGenerator(
                                                GeneratorConfig.builder()
                                                        .type(generatorType)
                                                        .version(defaultGeneratorConfig.defaultVersion())
                                                        .args(defaultGeneratorConfig.defaultArgs())
                                                        .build())
                                        .withProcessors(List.of(DefaultProcessorConfig.builder().build()))
                                        .build()))
                .build();
    }

    /**
     *
     *
     * @return {@code 0} in case the config file was generated successfully, {@code 1} in case a general error occurred
     *         that is not covered by more specific exit code, {@code 2} when a config validation failure occurred,
     *         {@code 3} when a base config could not be found
     */
    @Override
    public Integer call() throws Exception {
        // Make sure there is no context
        MDCUtils.removeContext();
        MDCUtils.addBuildContext(this.buildId);

        PncBuildConfig config = null;

        if (this.configPath != null) {
            log.debug("Trying to deserialize provided config at '{}'", this.configPath);

            config = ObjectMapperProvider.yaml().readValue(this.configPath.toFile(), PncBuildConfig.class);

            if (config.isEmpty()) {
                log.debug("Deserialized configuration is empty, will generate one.");
                config = null;
            } else {
                log.debug("Successfully deserialized provided configuration: '{}'", config);
            }
        }

        if (config != null && !Objects.equals(this.buildId, config.getBuildId())) {
            log.error(
                    "Provided PNC build identifier '{}' does not match the build identifier in the configuration provided as well: '{}'",
                    this.buildId,
                    config.getBuildId());
            return GenerationResult.ERR_CONFIG_INVALID.getCode();
        }

        Build build = pncService.getBuild(this.buildId);

        if (build == null) {
            throw new MissingPncBuildException("Could not retrieve PNC build '{}'", this.buildId);
        }

        if (config == null) {
            config = productConfig(build);
        }

        if (config == null) {
            log.info("Unable to retrieve config for  build '{}', initializing default configuration", build.getId());
            config = initializeDefaultConfig(build);
        }

        if (config == null) {
            log.warn("Could not initialize product configuration for '{}' build, exiting", build.getId());
            return GenerationResult.ERR_CONFIG_MISSING.getCode();
        }

        if (config.getEnvironment() == null) {
            Map<String, String> envConfig = environmentConfig(build);

            if (envConfig.isEmpty()) {
                log.error("Could not obtain environment attributes for the '{}' build!", build.getId());
                return GenerationResult.ERR_GENERAL.getCode();
            }

            config.setEnvironment(envConfig);
        }

        log.debug("RAW config: '{}'", ObjectMapperProvider.json().writeValueAsString(config));

        configAdjuster.adjust(config);

        config.setBuildId(build.getId());

        log.debug("Configuration adjusted, starting validation");

        ValidationResult result = configSchemaValidator.validate(config);

        if (!result.isValid()) {
            log.error("Configuration is not valid!");

            result.getErrors().forEach(msg -> log.error(msg));
            return GenerationResult.ERR_CONFIG_INVALID.getCode();
        }

        log.debug("Configuration is valid!");

        String configuration;

        log.debug("Using {} format", format);

        if (format.equals(ConfigFormat.JSON)) {

            configuration = configReader.getJsonObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(config);
        } else {
            configuration = configReader.getYamlObjectMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(config);
        }

        if (target != null) {
            Files.writeString(target, configuration);
            log.info("Configuration saved as '{}' file", target.toAbsolutePath());
        } else {
            System.out.println(configuration); // NOSONAR This is what we want, it's a CLI
        }

        return GenerationResult.SUCCESS.getCode();
    }
}
