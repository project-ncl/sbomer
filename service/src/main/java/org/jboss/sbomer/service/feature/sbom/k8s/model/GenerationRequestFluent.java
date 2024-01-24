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

import org.jboss.sbomer.core.features.sbom.enums.GenerationResult;
import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;

import io.fabric8.kubernetes.api.model.ConfigMapFluent;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

@SuppressWarnings(value = "unchecked")
public class GenerationRequestFluent<A extends GenerationRequestFluent<A>> extends ConfigMapFluent<A> {

    private String id;
    private String buildId;
    private SbomGenerationStatus status;
    private String reason;
    private String config;
    private String envConfig;
    private GenerationResult result;

    public ConfigMapFluent<A>.MetadataNested<A> withNewDefaultMetadata(String buildId) {
        return withNewMetadataLike(
                new ObjectMetaBuilder().withGenerateName("sbom-request-" + buildId.toLowerCase() + "-")
                        .withLabels(Labels.defaultLabelsToMap())
                        .build());
    }

    public A withBuildId(String buildId) {
        this.buildId = buildId;
        return (A) this;
    }

    public String getBuildId() {
        return buildId;
    }

    public A withId(String id) {
        this.id = id;
        return (A) this;
    }

    public String getId() {
        return id;
    }

    public A withStatus(SbomGenerationStatus status) {
        this.status = status;
        return (A) this;
    }

    public SbomGenerationStatus getStatus() {
        return status;
    }

    public A withReason(String reason) {
        this.reason = reason;
        return (A) this;
    }

    public String getReason() {
        return reason;
    }

    public A withEnvConfig(String envConfig) {
        this.envConfig = envConfig;
        return (A) this;
    }

    public String getEnvConfig() {
        return envConfig;
    }

    public A withConfig(String config) {
        this.config = config;
        return (A) this;
    }

    public String getConfig() {
        return config;
    }

    public A withResult(GenerationResult result) {
        this.result = result;
        return (A) this;
    }

    public GenerationResult getResult() {
        return result;
    }
}