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
package org.jboss.sbomer.cli.feature.sbom.command.auto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.jboss.sbomer.cli.feature.sbom.command.PathConverter;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbomer.config.runtime.Config;
import org.jboss.sbomer.core.features.sbomer.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbomer.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbomer.utils.ObjectMapperProvider;

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
        description = "Scripted SBOM generation using runtime configuration file being the output of the 'generate-config' command")
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

    @Override
    public Integer call() throws Exception {

        if (configPath != null) {
            log.info("Reading configuration file from '{}'", configPath.toAbsolutePath());

            if (!Files.exists(configPath)) {
                log.info("Configuration file '{}' does not exist", configPath.toAbsolutePath());
                return 2;
            }
        }

        // It is able to read both: JSON and YAML config files
        Config config = objectMapper.readValue(configPath.toAbsolutePath().toFile(), Config.class);

        log.debug("Configuration read successfully: {}", config);

        // Make sure there is no context
        MDCUtils.removeContext();
        MDCUtils.addBuildContext(config.getBuildId());

        if (index != null) {

            if (index < 0) {
                log.error("Provided index '{}' is lower than minimal required: 0", index);
                return 2;
            }

            if (index >= config.getProducts().size()) {
                log.error(
                        "Provided index '{}' is out of the available range [0-{}]",
                        index,
                        config.getProducts().size() - 1);
                return 2;
            }

            log.info("Running SBOM generation for product with index '{}'", index);

            generateSbom(config, config.getProducts().get(index));
        } else {
            log.debug(
                    "Generating SBOMs for all {} products defined in the runtime configuration",
                    config.getProducts().size());

            config.getProducts().forEach(product -> {
                generateSbom(config, product);
            });
        }

        return 0;
    }

    private void generateSbom(Config config, ProductConfig product) {
        log.debug("Product: '{}'", product.toString());

        List<String> command = product.generateCommand(config);
        Path outputDir = Path.of(workdir.toAbsolutePath().toString(), "product" + "-" + index);

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
        if (spec.root().commandLine().execute(command.toArray(new String[command.size()])) != 0) {
            throw new ApplicationException("Command '{}' failed, see logs above", cmd);
        }
    }
}
