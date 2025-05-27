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
package org.jboss.sbomer.service.v1beta2.generator;

import java.util.List;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

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
public class GeneratorConfigReader {

    public static final String CONFIG_KEY = "generators-config.yaml";

    KubernetesClient kubernetesClient;

    String sbomerReleaseName;

    @Inject
    public GeneratorConfigReader(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        this.sbomerReleaseName = ConfigProvider.getConfig()
                .getOptionalValue("SBOMER_RELEASE", String.class)
                .orElse("sbomer");
    }

    public List<GeneratorProfile> getGeneratorProfiles() {
        GeneratorsConfig config = getConfig();

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
    public GeneratorsConfig getConfig() {
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
