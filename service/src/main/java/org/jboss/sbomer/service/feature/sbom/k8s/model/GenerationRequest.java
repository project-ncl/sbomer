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
package org.jboss.sbomer.service.feature.sbom.k8s.model;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * This is a convenience model class. It is basically a {@link ConfigMap}, just with additional features to make it work
 * in the SBOM generation context better.
 * </p>
 *
 * <p>
 * Following labels are expect to be present on the {@link ConfigMap} resource in order for it to be used as
 * {@link GenerationRequest}.
 * <ul>
 * <li>{@code app.kubernetes.io/part-of=sbomer}</li>
 * <li>{@code app.kubernetes.io/component=sbom}</li>
 * <li>{@code app.kubernetes.io/managed-by=sbom}</li>
 * <li>{@code sbomer.jboss.org/generation-request}</li>
 * </ul>
 * </p>
 *
 * <p>
 * Additionally following labels can be added:
 *
 * <ul>
 * <li>{@code sbomer.jboss.org/sbom-build-id} -- the identifier of the build for which the generation is triggered</li>
 * </ul>
 * </p>
 */
@Kind("ConfigMap")
@Version("v1")
@Group("")
@Slf4j
public class GenerationRequest extends ConfigMap {

    public static final String KEY_ID = "id";
    public static final String KEY_BUILD_ID = "build-id";
    public static final String KEY_STATUS = "status";
    public static final String KEY_REASON = "reason";
    public static final String KEY_RESULT = "result";
    public static final String KEY_CONFIG = "config";

    public GenerationRequest() {
        super();
    }

    public GenerationRequest(
            String apiVersion,
            Map<String, String> binaryData,
            Map<String, String> data,
            Boolean immutable,
            String kind,
            ObjectMeta metadata) {
        super(apiVersion, binaryData, data, immutable, kind, metadata);
    }

    @JsonIgnore
    public String getId() {
        return getData().get(KEY_ID);
    }

    public void setId(String id) {
        getData().put(KEY_ID, id);
    }

    @JsonIgnore
    public String getBuildId() {
        return getData().get(KEY_BUILD_ID);
    }

    public void setBuildId(String buildId) {
        getData().put(KEY_BUILD_ID, buildId);
    }

    @JsonIgnore
    public String getConfig() {
        return getData().get(KEY_CONFIG);
    }

    public void setConfig(String config) {
        getData().put(KEY_CONFIG, config);
    }

    @JsonIgnore
    public String getReason() {
        return getData().get(KEY_REASON);
    }

    public void setReason(String reason) {
        getData().put(KEY_REASON, reason);
    }

    @JsonIgnore
    public GenerationResult getResult() {
        String resultStr = getData().get(KEY_RESULT);

        if (resultStr == null) {
            return null;
        }

        return GenerationResult.valueOf(resultStr);
    }

    public void setResult(GenerationResult result) {
        if (result == null) {
            return;
        }

        getData().put(KEY_RESULT, result.name());
    }

    @JsonIgnore
    public SbomGenerationStatus getStatus() {
        String statusStr = getData().get(KEY_STATUS);

        if (statusStr == null) {
            return null;
        }

        return SbomGenerationStatus.valueOf(statusStr);
    }

    public void setStatus(SbomGenerationStatus status) {
        if (status == null) {
            return;
        }

        getData().put(KEY_STATUS, status.name());
        getMetadata().getLabels().put(Labels.LABEL_PHASE, status.name());
    }

    @JsonIgnore
    public String dependentResourceName(SbomGenerationPhase phase) {
        return this.getMetadata().getName() + "-" + phase.ordinal() + "-" + phase.name().toLowerCase();
    }

    @JsonIgnore
    public String getName() {
        return getMetadata().getName();
    }

    @JsonIgnore
    public Config toConfig() {
        if (getConfig() == null) {
            return null;
        }

        try {
            return ObjectMapperProvider.yaml().readValue(getConfig().toString().getBytes(), Config.class);
        } catch (IOException e) {
            log.warn(e.getMessage(), e);
            return null;
        }
    }
}
