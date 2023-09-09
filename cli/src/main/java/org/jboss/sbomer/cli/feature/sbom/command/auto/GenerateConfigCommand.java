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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.sbomer.cli.feature.sbom.ConfigReader;
import org.jboss.sbomer.cli.feature.sbom.ProductVersionMapper;
import org.jboss.sbomer.cli.feature.sbom.command.PathConverter;
import org.jboss.sbomer.cli.feature.sbom.config.DefaultGenerationConfig;
import org.jboss.sbomer.cli.feature.sbom.config.DefaultGenerationConfig.DefaultGeneratorConfig;
import org.jboss.sbomer.cli.feature.sbom.config.DefaultProcessingConfig;
import org.jboss.sbomer.cli.feature.sbom.service.PncService;
import org.jboss.sbomer.core.SchemaValidator;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.config.runtime.DefaultProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

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

    public static enum ConfigFormat {
        yaml, json;
    }

    @Option(names = { "--build-id" }, required = true, description = "The PNC build identifier, example: AYHJRDPEUMYAC")
    String buildId;

    @Option(names = { "--format" }, defaultValue = "YAML", description = "Format of the generated configuration.")
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
    DefaultGenerationConfig defaultGenerationConfig;

    @Inject
    DefaultProcessingConfig defaultProcessingConfig;

    @Inject
    ProductVersionMapper productVersionMapper;

    private Config productConfig() {
        // Make sure there is no context
        MDCUtils.removeContext();
        MDCUtils.addBuildContext(buildId);

        log.info("Obtaining runtime configuration for build '{}'", buildId);

        // 1. Find if we can obtain the configuration from the file, if not found
        // 2. Find if we can find configuration in the mapper
        // 3. Try to use defaults (if possible)

        Config config = clientConfig();

        // Client configuration found, use it!
        if (config != null) {
            return config;
        }

        config = mappingConfig();

        // Mapping configuration found, use it!
        if (config != null) {
            return config;
        }

        log.warn("Runtime configuration for build '{}' could not be found", buildId);

        // Configuration file could not be found
        return null;
    }

    /**
     * Retrieves configuration from a SBOMer configuration file stored in the source code repository.
     *
     * @return {@link Config} object if the configuration could be retrieved or {@code null} otherwise.
     */
    private Config clientConfig() {
        log.debug("Attempting to fetch configuration from source code repository");

        Build build = pncService.getBuild(buildId);

        if (build == null) {
            log.warn("Could not retrieve PNC build '{}'", buildId);
            return null;
        }

        Config config = configReader.getConfig(build);

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
    private Config mappingConfig() {
        log.debug("Attempting to fetch configuration from SBOMer internal mapping");

        List<ProductVersionRef> productVersions = pncService.getProductVersions(buildId);

        if (productVersions.isEmpty()) {
            log.debug("Could not obtain PNC Product Version information for the '{}' PNC build", buildId);
            return null;
        }

        List<Config> configs = new ArrayList<>();

        productVersions.forEach(productVersion -> {
            log.debug(
                    "Trying to find configuration in internal mapping for product version {}",
                    productVersion.getId());

            Config config = productVersionMapper.getMapping().get(productVersion.getId());

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

        log.info("Found {} configurations in the internal SBOMer mapping for build ID '{}'", configs.size(), buildId);

        Config config = Config.builder()
                .buildId(buildId)
                .apiVersion("sbomer.jboss.org/v1alpha1")
                .products(new ArrayList<>())
                .build();

        configs.forEach(cfg -> {
            config.getProducts().addAll(cfg.getProducts());
        });

        log.info(
                "Found {} products in the internal SBOMer mapping for build ID '{}'",
                config.getProducts().size(),
                buildId);

        return config;
    }

    private GeneratorConfig defaultGeneratorConfig() {
        DefaultGeneratorConfig defaultGeneratorConfig = defaultGenerationConfig
                .forGenerator(defaultGenerationConfig.defaultGenerator());

        return GeneratorConfig.builder()
                .type(defaultGenerationConfig.defaultGenerator())
                .args(defaultGeneratorConfig.defaultArgs())
                .version(defaultGeneratorConfig.defaultVersion())
                .build();
    }

    private void adjustGenerator(ProductConfig product) {
        GeneratorConfig generatorConfig = product.getGenerator();

        GeneratorConfig defaultGeneratorConfig = defaultGeneratorConfig();

        // Generator configuration was not provided, will use defaults
        if (generatorConfig == null) {
            log.debug("No generator provided, will use defaults: '{}'", defaultGeneratorConfig);
            product.setGenerator(defaultGeneratorConfig);
        } else {

            if (generatorConfig.getVersion() == null) {
                String defaultVersion = defaultGenerationConfig.forGenerator(generatorConfig.getType())
                        .defaultVersion();

                log.debug("No generator version provided, will use default: '{}'", defaultVersion);
                generatorConfig.setVersion(defaultVersion);
            }

            if (generatorConfig.getArgs() == null) {
                String defaultArgs = defaultGenerationConfig.forGenerator(generatorConfig.getType()).defaultArgs();

                log.debug("No generator args provided, will use default: '{}'", defaultArgs);
                generatorConfig.setArgs(defaultArgs);
            }
        }
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
        Config config = productConfig();

        if (config == null) {
            log.error("Could not obtain product configuration for the '{}' build, exiting", buildId);
            return GenerationResult.ERR_CONFIG_MISSING.getCode();
        }

        log.debug("RAW config: '{}'", config);

        log.debug("Adjusting configuration...");

        config.getProducts().forEach(product -> {
            // Adjusting generator configuration. This is the only thing we can adjust,
            // because processor configuration is specific to the build and product release.
            adjustGenerator(product);

            if (!product.hasDefaultProcessor()) {
                // Adding default processor as the first one
                log.debug("No default processor specified, adding one");
                product.getProcessors().add(0, new DefaultProcessorConfig());
            }
        });

        config.setBuildId(buildId);

        log.debug("Configuration adjusted, starting validation");

        ValidationResult result = validate(config);

        if (!result.isValid()) {
            log.error("Configuration is not valid!");

            result.getErrors().forEach(msg -> {
                log.error(msg);
            });
            return GenerationResult.ERR_CONFIG_INVALID.getCode();
        }

        log.debug("Configuration is valid!");

        String configuration;

        log.debug("Using {} format", format);

        if (format.equals(ConfigFormat.json)) {

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
            System.out.println(configuration);
        }

        return GenerationResult.SUCCESS.getCode();
    }

    /**
     * Performs validation of a give {@link Config} according to the JSON schema.
     *
     * @param config The {@link Config} object to validate.
     * @return a {@link org.jboss.sbomer.core.SchemaValidator.ValidationResult} object.
     */
    private ValidationResult validate(Config config) {
        String schema;

        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("schemas/config.json");
            schema = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApplicationException("Could not read the configuration file schema", e);
        }

        try {
            return SchemaValidator.validate(schema, configReader.getJsonObjectMapper().writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new ApplicationException("An error occurred while converting configuration file into JSON", e);
        }
    }

}
