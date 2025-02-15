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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jboss.sbomer.cli.feature.sbom.command.PathConverter;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "generate",
        description = "Scripted SBOM generation using runtime configuration file being the output of the 'generate-env-config' command")
public class GenerateCommand implements Callable<Integer> {
    @Option(
            names = { "-c", "--config", },
            paramLabel = "FILE",
            description = "Location of the runtime configuration file.",
            required = true)
    Path configPath;

    @Option(
            names = { "--index" },
            description = "Index to select the product configuration passed in the --config option. Starts from 0. If not provided SBOM will be generated for every product in the config serially.")
    Integer index;

    @Option(
            names = { "--workdir" },
            defaultValue = "workdir",
            paramLabel = "DIR",
            description = "The directory where the generation should be performed. Default: ${DEFAULT-VALUE}",
            converter = PathConverter.class)
    Path workdir;

    @Option(
            names = { "-f", "--force" },
            description = "If the workdir directory should be cleaned up in case it already exists. Default: ${DEFAULT-VALUE}")
    boolean force = false;

    @Spec
    CommandSpec spec;

    ObjectMapper objectMapper = ObjectMapperProvider.yaml();

    /**
     * @return {@code 0} in case the generation process finished successfully, {@code 1} in case a general error
     *         occurred that is not covered by more specific exit code, {@code 2} when a problem related to
     *         configuration file reading or parsing, {@code 3} when the index parameter is incorrect, {@code 4} when
     *         the generation process did not finish successfully
     */
    @Override
    public Integer call() {

        if (configPath == null) {
            log.info("Configuration path is null, cannot do any generation.");
            return GenerationResult.ERR_CONFIG_MISSING.getCode();
        }

        log.info("Reading configuration file from '{}'", configPath.toAbsolutePath());

        if (!Files.exists(configPath)) {
            log.info("Configuration file '{}' does not exist", configPath.toAbsolutePath());
            return GenerationResult.ERR_CONFIG_MISSING.getCode();
        }

        // It is able to read both: JSON and YAML config files
        PncBuildConfig config;

        try {
            config = objectMapper.readValue(configPath.toAbsolutePath().toFile(), PncBuildConfig.class);
        } catch (StreamReadException e) {
            log.error("Unable to parse the configuration file", e);
            return GenerationResult.ERR_CONFIG_INVALID.getCode();
        } catch (DatabindException e) {
            log.error("Unable to deserialize the configuration file", e);
            return GenerationResult.ERR_CONFIG_INVALID.getCode();
        } catch (IOException e) {
            log.error("Unable to read configuration file", e);
            return GenerationResult.ERR_CONFIG_INVALID.getCode();
        }

        log.debug("Configuration read successfully: {}", config);

        // Make sure there is no context
        MDCUtils.removeContext();
        MDCUtils.addBuildContext(config.getBuildId());

        if (index != null) {

            if (index < 0) {
                log.error("Provided index '{}' is lower than minimal required: 0", index);
                return GenerationResult.ERR_INDEX_INVALID.getCode();
            }

            if (index >= config.getProducts().size()) {
                log.error(
                        "Provided index '{}' is out of the available range [0-{}]",
                        index,
                        config.getProducts().size() - 1);
                return GenerationResult.ERR_INDEX_INVALID.getCode();
            }

            log.info("Running SBOM generation for product with index '{}'", index);

            try {
                generateSbom(config, config.getProducts().get(index), index);
            } catch (ApplicationException e) {
                log.error("Generation process failed", e);
                return GenerationResult.ERR_GENERATION.getCode();
            }
        } else {
            log.debug(
                    "Generating SBOMs for all {} products defined in the runtime configuration",
                    config.getProducts().size());

            for (int i = 0; i < config.getProducts().size(); i++) {
                try {
                    generateSbom(config, config.getProducts().get(i), i);
                } catch (ApplicationException e) {
                    log.error("Generation process failed", e);
                    return GenerationResult.ERR_GENERATION.getCode();
                }
            }
        }

        return GenerationResult.SUCCESS.getCode();
    }

    private void generateSbom(PncBuildConfig config, ProductConfig product, int i) {
        log.debug("Product: '{}'", product.toString());

        List<String> command = product.generateCommand(config);
        Path outputDir = Path.of(workdir.toAbsolutePath().toString(), "product" + "-" + i);

        command.add("--workdir");
        command.add(outputDir.toAbsolutePath().toString());

        if (force) {
            command.add("--force");
        }

        log.debug("To run: {}", command);

        String cmd = command.stream().map(param -> {
            if (param.contains(" ")) {
                return "\"" + param + "\"";
            }

            return param;
        }).collect(Collectors.joining(" "));

        log.info("Running: '{}'", cmd);

        // Execute the generation
        if (spec.root().commandLine().execute(command.toArray(new String[0])) != 0) {
            throw new ApplicationException("Command '{}' failed, see logs above", cmd);
        }
    }
}
