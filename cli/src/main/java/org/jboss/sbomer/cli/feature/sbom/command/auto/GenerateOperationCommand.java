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
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.pnc.PncService;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * <p>
 * Command to generate a deliverable pom for SBOMer generation
 * </p>
 */
@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "generate-operation",
        description = "Prepares the SBOM for a given PNC operation")
public class GenerateOperationCommand implements Callable<Integer> {

    @Option(
            names = { "-c", "--config", },
            paramLabel = "FILE",
            description = "Location of the runtime operation configuration file.",
            required = true)
    Path configPath;

    @Option(
            names = { "--index" },
            description = "Index to select the deliverable configuration passed in the --config option. Starts from 0. If not provided POMs will be generated for every deliverable in the config serially")
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

    @Inject
    PncService pncService;

    /**
     * @return {@code 0} in case the generation process finished successfully, {@code 1} in case a general error
     *         occurred that is not covered by more specific exit code, {@code 2} when a problem related to
     *         configuration file reading or parsing, {@code 3} when the index parameter is incorrect, {@code 4} when
     *         generation process did not finish successfully
     */
    @Override
    public Integer call() throws Exception {

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
        OperationConfig config;

        try {
            config = ObjectMapperProvider.json().readValue(configPath.toAbsolutePath().toFile(), OperationConfig.class);
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

        List<String> deliverableUrls = config.getDeliverableUrls();
        if (index != null) {

            if (index < 0) {
                log.error("Provided index '{}' is lower than minimal required: 0", index);
                return GenerationResult.ERR_INDEX_INVALID.getCode();
            }

            if (index >= deliverableUrls.size()) {
                log.error(
                        "Provided index '{}' is out of the available range [0-{}]",
                        index,
                        deliverableUrls.size() - 1);
                return GenerationResult.ERR_INDEX_INVALID.getCode();
            }

            log.info("Running SBOM generation for deliverable with index '{}'", index);

            try {
                generateDeliverableSbom(config, index);
            } catch (ApplicationException e) {
                log.error("Generation process failed", e);
                return GenerationResult.ERR_GENERATION.getCode();
            }
        } else {
            log.debug(
                    "Generating SBOM for all {} deliverables defined in the runtime configuration",
                    deliverableUrls.size());

            for (int i = 0; i < deliverableUrls.size(); i++) {
                try {
                    generateDeliverableSbom(config, i);
                } catch (ApplicationException e) {
                    log.error("Generation process failed", e);
                    return GenerationResult.ERR_GENERATION.getCode();
                }
            }
        }

        return GenerationResult.SUCCESS.getCode();
    }

    private void generateDeliverableSbom(OperationConfig config, int i) {
        log.info(
                "Generating deliverable SBOM for the deliverable: {}, with index: {}, with the provided config: {}",
                config.getDeliverableUrls().get(i),
                i,
                config);

        List<String> command = config.getProduct().generateCommand(config);
        command.add("--config");
        command.add(configPath.toAbsolutePath().toString());

        command.add("--index");
        command.add(String.valueOf(index));

        command.add("--workdir");
        command.add(workdir.toAbsolutePath().toString());

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
