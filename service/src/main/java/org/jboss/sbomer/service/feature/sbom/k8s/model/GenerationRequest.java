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
package org.jboss.sbomer.service.feature.sbom.k8s.model;

import java.util.Map;

import org.jboss.pnc.common.otel.OtelUtils;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.config.Config;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Kind;
import io.fabric8.kubernetes.model.annotation.Version;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;

import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACE_ID_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_SPAN_ID_KEY;
import static org.jboss.sbomer.core.features.sbom.utils.MDCUtils.MDC_TRACEPARENT_KEY;

/**
 * <p>
 * This is a convenience model class. It is basically a {@link ConfigMap}, just with additional features to make it work
 * in the SBOM generation context better.
 * </p>
 *
 * <p>
 * Following labels are expected to be present on the {@link ConfigMap} resource in order for it to be used as
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
 * Additionally, the following labels can be added:
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
    public static final String KEY_TYPE = "type";
    public static final String KEY_IDENTIFIER = "identifier";

    public static final String KEY_STATUS = "status";
    public static final String KEY_REASON = "reason";
    public static final String KEY_RESULT = "result";
    public static final String KEY_CONFIG = "config";
    public static final String KEY_ENV_CONFIG = "env-config";

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
    public GenerationRequestType getType() {
        String typeStr = getData().get(KEY_TYPE);

        if (typeStr == null) {
            return null;
        }

        return GenerationRequestType.fromName(typeStr);
    }

    public void setType(GenerationRequestType type) {
        if (type == null) {
            return;
        }

        getData().put(KEY_TYPE, type.toName());
        getMetadata().getLabels().put(Labels.LABEL_TYPE, type.toName());
    }

    @JsonIgnore
    public String getId() {
        return getData().get(KEY_ID);
    }

    public void setId(String id) {
        getData().put(KEY_ID, id);
    }

    @JsonIgnore
    public String getIdentifier() {
        return getData().get(KEY_IDENTIFIER);
    }

    public void setIdentifier(String identifier) {
        getData().put(KEY_IDENTIFIER, identifier);
    }

    @JsonIgnore
    public Config getConfig() {
        String configData = getData().get(KEY_CONFIG);
        return Config.fromString(configData);
    }

    /**
     * @deprecated Use {@link #getConfig()} instead.
     */
    @JsonIgnore
    @Deprecated(since = "1.0.0", forRemoval = true)
    public <T extends Config> T getConfig(Class<T> clazz) {
        return getConfig(clazz, false);
    }

    @JsonIgnore
    public <T extends Config> T getConfig(Class<T> clazz, boolean def) {
        String configData = getData().get(KEY_CONFIG);
        T config = Config.fromString(configData, clazz);

        if (config == null && def) {
            return Config.newInstance(clazz);
        }
        return config;
    }

    @JsonIgnore
    public String getJsonConfig() {
        Config config = this.getConfig();

        if (config == null) {
            return null;
        }

        return config.toJson();
    }

    public void setConfig(String config) {
        getData().put(KEY_CONFIG, config);
    }

    public void setConfig(Config config) {
        try {
            getData().put(KEY_CONFIG, ObjectMapperProvider.json().writeValueAsString(config));
        } catch (JsonProcessingException e) {
            throw new ApplicationException(
                    "Cannot convert configuration into a JSON string: '{}'",
                    this.getMetadata().getName(),
                    config,
                    e);
        }
    }

    @JsonIgnore
    public String getEnvConfig() {
        return getData().get(KEY_ENV_CONFIG);
    }

    public void setEnvConfig(String envConfig) {
        getData().put(KEY_ENV_CONFIG, envConfig);
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
            getData().remove(KEY_STATUS);
            getMetadata().getLabels().remove(Labels.LABEL_STATUS);
            return;
        }

        getData().put(KEY_STATUS, status.name());
        getMetadata().getLabels().put(Labels.LABEL_STATUS, status.name());
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
    public String getTraceId() {
        return getMetadata().getLabels().get(Labels.LABEL_OTEL_TRACE_ID);
    }

    @JsonIgnore
    public String getSpanId() {
        return getMetadata().getLabels().get(Labels.LABEL_OTEL_SPAN_ID);
    }

    @JsonIgnore
    public String getTraceParent() {
        return getMetadata().getLabels().get(Labels.LABEL_OTEL_TRACEPARENT);
    }

    @JsonIgnore
    public Map<String, String> getMDCOtel() {
        String traceId = getTraceId() != null ? getTraceId() : Span.getInvalid().getSpanContext().getTraceId();
        String spanId = getSpanId() != null ? getSpanId() : Span.getInvalid().getSpanContext().getSpanId();
        String traceParent = getTraceParent() != null ? getTraceParent()
                : OtelUtils
                        .createTraceParent(traceId, spanId, Span.getInvalid().getSpanContext().getTraceFlags().asHex());

        return Map.of(MDC_TRACE_ID_KEY, traceId, MDC_SPAN_ID_KEY, spanId, MDC_TRACEPARENT_KEY, traceParent);
    }
}
