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

import org.jboss.sbomer.service.feature.sbom.k8s.resources.Labels;

import io.fabric8.kubernetes.api.model.ConfigMapFluent;
import io.fabric8.kubernetes.api.model.ConfigMapFluentImpl;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

@SuppressWarnings(value = "unchecked")
public class GenerationRequestFluentImpl<A extends GenerationRequestFluent<A>> extends ConfigMapFluentImpl<A>
        implements GenerationRequestFluent<A> {

    private String id;
    private String buildId;
    private SbomGenerationStatus status;

    @Override
    public ConfigMapFluent.MetadataNested<A> withNewDefaultMetadata(String buildId) {
        return withNewMetadataLike(
                new ObjectMetaBuilder().withName("sbom-request-" + buildId.toLowerCase())
                        .withLabels(Labels.defaultLabelsToMap())
                        .build());
    }

    @Override
    public A withBuildId(String buildId) {
        this.buildId = buildId;
        return (A) this;
    }

    public String getBuildId() {
        return buildId;
    }

    @Override
    public A withId(String id) {
        this.id = id;
        return (A) this;
    }

    public String getId() {
        return id;
    }

    @Override
    public A withStatus(SbomGenerationStatus status) {
        this.status = status;
        return (A) this;
    }

    public SbomGenerationStatus getStatus() {
        return status;
    }

}