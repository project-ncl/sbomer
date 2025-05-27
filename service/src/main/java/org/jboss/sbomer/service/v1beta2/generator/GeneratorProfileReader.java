package org.jboss.sbomer.service.v1beta2.generator;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@NoArgsConstructor
@Slf4j
public class GeneratorProfileReader {

    public static final String PROFILE_KEY = "generatorProfiles";

    KubernetesClient kubernetesClient;

    String sbomerReleaseName;

    @Inject
    public GeneratorProfileReader(KubernetesClient kubernetesClient) {
        this.kubernetesClient = kubernetesClient;
        this.sbomerReleaseName = ConfigProvider.getConfig()
                .getOptionalValue("SBOMER_RELEASE", String.class)
                .orElse("sbomer");
    }

    /**
     * <p>
     * Reads and deserializes generation profiles which are currently supported generators.
     * </p>
     * 
     * <p>
     * It provides additional configuration and defaults as well.
     * </p>
     * 
     * @return List of generator profiles.
     */
    public List<GeneratorProfile> getProfiles() {
        String content = getCmContent(cmName());

        if (content == null) {
            log.warn("Could not read '{}' ConfigMap content", cmName());
            return null;
        }

        List<GeneratorProfile> profiles = null;

        try {
            profiles = ObjectMapperProvider.yaml().readValue(content, new TypeReference<List<GeneratorProfile>>() {
            });
        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to parse generator profiles from ConfigMap '{}' using key '{}', profiles won't be applied!",
                    cmName(),
                    PROFILE_KEY,
                    e);

            return null;
        }

        return profiles;
    }

    /**
     * Returns the content of the ConfigMap as a {@code String}.
     * 
     * @return CM content
     */
    private String getCmContent(String configMapName) {
        ConfigMap configMap = kubernetesClient.configMaps().withName(configMapName).get();

        if (configMap == null) {
            log.debug("Could not find '{}' ConfigMap", configMapName);
            return null;
        }

        Map<String, String> data = configMap.getData();

        if (data == null || !data.containsKey(PROFILE_KEY)) {
            log.debug("'{}' ConfigMap is empty or does not contain the '{}' key", configMapName, PROFILE_KEY);
            return null;
        }

        return data.get(PROFILE_KEY);
    }

    /**
     * Constructs the ConfigMap name based on the SBOMer release name.
     * 
     * @return Name of the ConfigMap holding generation profiles.
     */
    private String cmName() {
        return sbomerReleaseName + "-generator-profiles";
    }
}
