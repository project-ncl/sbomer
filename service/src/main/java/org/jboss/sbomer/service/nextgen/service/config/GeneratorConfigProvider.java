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
package org.jboss.sbomer.service.nextgen.service.config;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jboss.sbomer.core.SchemaValidator;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.ValidationException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GenerationRequestSpec;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GeneratorConfigSpec;
import org.jboss.sbomer.service.nextgen.core.payloads.generation.GeneratorVersionConfigSpec;
import org.jboss.sbomer.service.nextgen.core.utils.ConfigUtils;
import org.jboss.sbomer.service.nextgen.core.utils.JacksonUtils;
import org.jboss.sbomer.service.nextgen.service.config.mapping.DefaultGeneratorMappingEntry;
import org.jboss.sbomer.service.nextgen.service.config.mapping.GeneratorProfile;
import org.jboss.sbomer.service.nextgen.service.config.mapping.GeneratorVersionProfile;
import org.jboss.sbomer.service.nextgen.service.config.mapping.GeneratorsConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class GeneratorConfigProvider {

    public static final String CONFIG_KEY = "generators-config.yaml";

    KubernetesClient kubernetesClient;

    String sbomerReleaseName;

    protected GeneratorsConfig config;

    public GeneratorsConfig getConfig() {
        if (config == null) {
            updateConfig();
        }

        return config;
    }

    @Inject
    public GeneratorConfigProvider(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        this.sbomerReleaseName = ConfigUtils.getRelease();
    }

    /**
     * <p>
     * Reads, deserializes and stores generation config.
     * </p>
     *
     */
    @Scheduled(every = "20s", delay = 10, delayUnit = TimeUnit.SECONDS, concurrentExecution = ConcurrentExecution.SKIP)
    public void updateConfig() {
        String content = getCmContent(cmName());

        if (content == null) {
            log.warn("Could not read '{}' ConfigMap content, see logs for more information", cmName());
            throw new ApplicationException(
                    "Could not read generators config, please make sure the system is properly configured, unable to process request");
        }

        GeneratorsConfig config = null;

        try {
            config = ObjectMapperProvider.yaml().readValue(content, GeneratorsConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse generator config from ConfigMap '{}', profiles won't be applied!", cmName(), e);

            throw new ApplicationException(
                    "Could not read generators config, please make sure the system is properly configured, unable to process request");
        }

        this.config = config;
    }

    /**
     * Fetches generator profile for a given generator name and version. If version is not provided, default version
     * will be used.
     * 
     * @param name Generator name
     * @param version Generator version
     * @return {@link GeneratorVersionProfile} representing the configuration
     */
    private GeneratorVersionProfile getGeneratorProfile(String name, String version) {
        if (name == null || name.isBlank()) {
            throw new ApplicationException("No generator name specified, cannot continue");
        }

        log.debug("Finding generator profile for '{}' name", name);

        // Find the profile for this generator
        Optional<GeneratorProfile> generatorProfileOpt = getConfig().generatorProfiles()
                .stream()
                .filter(p -> p.name().equals(name))
                .findFirst();

        // If we did not find anything, fail
        if (generatorProfileOpt.isEmpty()) {
            throw new ApplicationException("No generator profile found for '{}' generator", name);
        }

        GeneratorProfile generatorProfile = generatorProfileOpt.get();

        log.debug("Found a profile for generator '{}', obtaining profile version now...", generatorProfile.name());

        GeneratorVersionProfile versionProfile = null;

        // If a specific version is requested
        if (version != null) {
            log.debug("Trying to find a profile version for '{}'", version);

            Optional<GeneratorVersionProfile> versionProfileOpt = generatorProfile.versions()
                    .stream()
                    .filter(pv -> pv.version().equals(version))
                    .findFirst();

            if (versionProfileOpt.isEmpty()) {
                throw new ClientException(
                        "Could not find requested version: '{}' for generator '{}'",
                        version,
                        generatorProfile.name());
            }

            versionProfile = versionProfileOpt.get();
        } else {
            // If no version is specified -- use default version which is the first version in the config
            versionProfile = generatorProfile.versions().get(0);
        }

        return versionProfile;
    }

    public GenerationRequestSpec buildEffectiveRequest(GenerationRequestSpec requestSpec) {
        if (requestSpec == null) {
            log.error("No request specification provided, this should not happen");
            throw new ClientException("No request specification provided, please make sure your request is correct.");
        }

        log.debug("Will build effective config for provided request: {}", requestSpec);
        log.debug("Searching for generator that support requested '{}' type", requestSpec.target().type());

        Optional<DefaultGeneratorMappingEntry> generatorMappingForSelectedTypeOpt = getConfig()
                .defaultGeneratorMappings()
                .stream()
                .filter(m -> m.targetType().equals(requestSpec.target().type()))
                .findFirst(); // TODO: Shouldn't be a list actually?

        if (generatorMappingForSelectedTypeOpt.isEmpty()) {
            throw new ClientException(
                    "Provided target: '{}' is not supported. Supported targets: {}",
                    requestSpec.target().type(),
                    getConfig().defaultGeneratorMappings().stream().map(m -> m.targetType()).toList());
        }

        String generatorName = null;

        // In case the generator was not specified within the request we will use defaults
        if (requestSpec.generator() == null || requestSpec.generator().name() == null
                || requestSpec.generator().name().isBlank()) {
            log.debug(
                    "No generator selected within the request, will use best generator available for '{}'",
                    requestSpec.target().type());

            // Select the best generator (name) for a given target type
            generatorName = generatorMappingForSelectedTypeOpt.get().generators().get(0);

            log.debug("Default generator for {} type is: '{}', will use it", generatorName);
        } else {
            generatorName = requestSpec.generator().name();

            log.debug("Using requested generator: '{}'", generatorName);
        }

        GeneratorVersionProfile generatorVersionProfile = getGeneratorProfile(
                generatorName,
                Optional.ofNullable(requestSpec.generator()).map(GeneratorVersionConfigSpec::version).orElse(null));

        log.debug("Using generator '{}' with version '{}'", generatorName, generatorVersionProfile.version());

        GeneratorConfigSpec configSpec = null;

        if (requestSpec.generator() != null && requestSpec.generator().config() != null) {
            log.debug("Generator configuration was provided, will use it");

            ObjectNode mergedConfig = JacksonUtils.merge(
                    JacksonUtils.toObjectNode(generatorVersionProfile.defaultConfig()),
                    JacksonUtils.toObjectNode(requestSpec.generator().config()));

            log.trace("Merged config: {}", mergedConfig);

            configSpec = JacksonUtils.parse(GeneratorConfigSpec.class, mergedConfig);
        } else {
            log.debug(
                    "Using default configuration for generator '{}' and version '{}'",
                    generatorName,
                    generatorVersionProfile.version());
            configSpec = generatorVersionProfile.defaultConfig();
        }

        // Prepare effective configuration which will be passed to the generator
        GenerationRequestSpec effectiveRequest = new GenerationRequestSpec(
                requestSpec.target(),
                new GeneratorVersionConfigSpec(generatorName, generatorVersionProfile.version(), configSpec));

        // Schema is provided, let's validate it!
        if (generatorVersionProfile.schema() != null) {

            String request;

            try {
                request = ObjectMapperProvider.json().writeValueAsString(effectiveRequest);
            } catch (JsonProcessingException e) {
                throw new ApplicationException("Unable to serialize request", e);
            }

            ValidationResult result = SchemaValidator.validate(generatorVersionProfile.schema().toString(), request);

            if (!result.isValid()) {
                throw new ValidationException(
                        "Effective configuration for the  generation zis not valid",
                        result.getErrors());
            }
        }

        return effectiveRequest;
    }

    /**
     * Returns the content of the ConfigMap particular key as a {@code String}.
     *
     * @return CM content
     */
    private String getCmContent(String configMapName) {
        ConfigMap configMap = kubernetesClient.configMaps().withName(configMapName).get();

        if (configMap == null) {
            log.debug("Could not find '{}' ConfigMap", configMapName);
            return null;
        }

        if (configMap.getData() == null) {
            log.debug("'{}' ConfigMap content is empty", configMapName);
            return null;
        }

        if (configMap.getData().get(CONFIG_KEY) == null) {
            log.debug("'{}' ConfigMap does not contain the '{}' key", configMapName, CONFIG_KEY);
            return null;
        }

        return configMap.getData().get(CONFIG_KEY);
    }

    /**
     * Constructs the ConfigMap name for profiles based on the SBOMer release name.
     *
     * @return Name of the ConfigMap holding generation profiles.
     */
    private String cmName() {
        return sbomerReleaseName + "-generators-config";
    }

}
