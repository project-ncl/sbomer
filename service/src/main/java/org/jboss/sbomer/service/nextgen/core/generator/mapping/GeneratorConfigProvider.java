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
package org.jboss.sbomer.service.nextgen.core.generator.mapping;

import java.util.List;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.sbomer.core.SchemaValidator;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.errors.ValidationException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GenerationRequestSpec;
import org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation.GeneratorVersionConfigSpec;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@NoArgsConstructor
@Slf4j
public class GeneratorConfigProvider {

    public static final String CONFIG_KEY = "generators-config.yaml";

    KubernetesClient kubernetesClient;

    String sbomerReleaseName;

    @Inject
    public GeneratorConfigProvider(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        this.sbomerReleaseName = ConfigProvider.getConfig()
                .getOptionalValue("SBOMER_RELEASE", String.class)
                .orElse("sbomer");
    }

    public List<GeneratorProfile> getGeneratorProfiles() {
        GeneratorsConfig config = getGeneratorsConfig();

        return config.generatorProfiles();
    }

    /**
     * <p>
     * Reads and deserializes generation config.
     * </p>
     *
     * <p>
     * It provides additional configuration and defaults as well.
     * </p>
     *
     * @return Generators configs and defaults.
     */
    public GeneratorsConfig getGeneratorsConfig() {
        // TODO: Add caching
        String content = getCmContent(cmName());

        if (content == null) {
            log.warn("Could not read '{}' ConfigMap content, see logs for more information", cmName());
            return null;
        }

        GeneratorsConfig config = null;

        try {
            config = ObjectMapperProvider.yaml().readValue(content, GeneratorsConfig.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse generator config from ConfigMap '{}', profiles won't be applied!", cmName(), e);

            return null;
        }

        return config;
    }

    public GenerationRequestSpec buildEffectiveRequest(GenerationRequestSpec requestSpec) {
        if (requestSpec == null) {
            log.error("No request specification provided, this should not happen");
            throw new ClientException("No request specification provided, please make sure your request is correct.");
        }

        log.debug("Will build effective config for provided request: {}", requestSpec);

        // Read all enabled generators
        GeneratorsConfig sbomerGeneratorsConfig = getGeneratorsConfig();

        // Ouch, we don't have any generators registered
        if (sbomerGeneratorsConfig == null) {
            throw new ApplicationException(
                    "Could not read generators config, please make sure the system is properly configured, unable to process request");
        }

        log.debug("Searching for generators that support requested '{}' type", requestSpec.target().type());

        Optional<DefaultGeneratorMappingEntry> generatorMappingForSelectedTypeOpt = sbomerGeneratorsConfig
                .defaultGeneratorMappings()
                .stream()
                .filter(m -> m.targetType().equals(requestSpec.target().type()))
                .findFirst();

        if (generatorMappingForSelectedTypeOpt.isEmpty()) {
            throw new ClientException(
                    "Provided target: '{}' is not supported. Supported targets: {}",
                    requestSpec.target().type(),
                    sbomerGeneratorsConfig.defaultGeneratorMappings().stream().map(m -> m.targetType()).toList());
        }

        GeneratorProfile generatorProfile = null;
        GeneratorVersionProfile generatorVersionProfile = null;

        if (requestSpec.generator() != null && requestSpec.generator().name() != null) {
            Optional<GeneratorProfile> generatorProfileOpt = sbomerGeneratorsConfig.generatorProfiles()
                    .stream()
                    .filter(p -> p.name().equals(requestSpec.generator().name()))
                    .findFirst();

            // If we did not find anything, fail
            if (generatorProfileOpt.isEmpty()) {
                throw new ApplicationException(
                        "Selected generator '{}' is not registered",
                        requestSpec.generator().name());
            }

            generatorProfile = generatorProfileOpt.get();

            // Find the best version of selected generator
            generatorVersionProfile = generatorProfile.versions().get(0);

        } else {

            // TODO: support taking into account provided partial config (merging)

            log.info(
                    "No generator selected within the request for type '{}', will try to find best generator",
                    requestSpec.target().type());

            DefaultGeneratorMappingEntry generatorMappingForSelectedType = generatorMappingForSelectedTypeOpt.get();

            List<String> generators = generatorMappingForSelectedType.generators();

            if (generators == null || generators.isEmpty()) {
                throw new ClientException(
                        "Unable to determine best generator, selected target type: '{}' is supported, but does not have default generator configured and thus needs to be provided explicitly as part of the request with requests[].generator.name field",
                        requestSpec.target().type());

            }

            // Select the best generator (name) for a given target type
            String defaultGenerator = generators.get(0);

            log.info("Will use '{}' generator, trying to find best profile", defaultGenerator);

            // Now find the profile for this generator
            Optional<GeneratorProfile> generatorProfileOpt = sbomerGeneratorsConfig.generatorProfiles()
                    .stream()
                    .filter(p -> p.name().equals(defaultGenerator))
                    .findFirst();

            // If we did not find anything, fail
            if (generatorProfileOpt.isEmpty()) {
                throw new ApplicationException("There are no generator profiles for '{}' generator", defaultGenerator);
            }

            generatorProfile = generatorProfileOpt.get();

            // Find the best version of selected generator
            generatorVersionProfile = generatorProfile.versions().get(0);
        }

        log.info("Using generator '{}' with version '{}'", generatorProfile.name(), generatorVersionProfile.version());

        // Prepare effective configuration which will be passed to the generator
        GenerationRequestSpec effectiveRequest = new GenerationRequestSpec(
                requestSpec.target(),
                new GeneratorVersionConfigSpec(
                        generatorProfile.name(),
                        generatorVersionProfile.version(),
                        generatorVersionProfile.defaultConfig()));

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

    public GeneratorVersionConfigSpec buildEffectiveConfig(GenerationRequestSpec requestSpec) {
        if (requestSpec == null) {
            log.error("No request specification provided, this should not happen");
            throw new ClientException("No request specification provided, please make sure your request is correct.");
        }

        log.debug("Will build effective config for provided request: {}", requestSpec);

        // Read all enabled generators
        GeneratorsConfig sbomerGeneratorsConfig = getGeneratorsConfig();

        // Ouch, we don't have any generators registered
        if (sbomerGeneratorsConfig == null) {
            throw new ApplicationException(
                    "Could not read generators config, please make sure the system is properly configured, unable to process request");
        }

        log.debug("Searching for generators that support requested '{}' type", requestSpec.target().type());

        Optional<DefaultGeneratorMappingEntry> generatorMappingOpt = sbomerGeneratorsConfig.defaultGeneratorMappings()
                .stream()
                .filter(m -> m.targetType().equals(requestSpec.target().type()))
                .findFirst();

        if (generatorMappingOpt.isEmpty()) {
            throw new ClientException(
                    "Provided target: '{}' is not supported. Supported targets: {}",
                    requestSpec.target().type(),
                    sbomerGeneratorsConfig.defaultGeneratorMappings().stream().map(m -> m.targetType()).toList());
        }

        if (requestSpec.generator() == null) {
            log.info(
                    "No generator selected within the request, will use best generator available for '{}'",
                    requestSpec.target().type());
        } else {
            // TODO: merge configs, currently ignored entirely
        }

        // Select the best generator (name) for a given target type
        String defaultGenerator = generatorMappingOpt.get().generators().get(0);

        // Now find the profile for this generator
        Optional<GeneratorProfile> generatorProfileOpt = sbomerGeneratorsConfig.generatorProfiles()
                .stream()
                .filter(p -> p.name().equals(defaultGenerator))
                .findFirst();

        // If we did not find anything, fail
        if (generatorProfileOpt.isEmpty()) {
            throw new ApplicationException("There is not generator profile for '{}' generator", defaultGenerator);
        }

        GeneratorProfile generatorProfile = generatorProfileOpt.get();

        // Find the best version of selected generator
        GeneratorVersionProfile generatorVersionProfile = generatorProfile.versions().get(0);

        log.info("Using generator '{}' with version '{}'", generatorProfile.name(), generatorVersionProfile.version());

        // Prepare effective configuration which will be passed to the generator
        return new GeneratorVersionConfigSpec(
                generatorProfile.name(),
                generatorVersionProfile.version(),
                generatorVersionProfile.defaultConfig());
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
