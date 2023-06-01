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
package org.jboss.sbomer.feature.sbom.event;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.sbomer.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.feature.sbom.k8s.model.SbomGenerationStatus;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class BuildFinishedEventListener {

    @Inject
    KubernetesClient kubernetesClient;

    public void init(@Observes StartupEvent ev) {
        // TODO how to make these querable? add more labels?
        GenerationRequest req = new GenerationRequestBuilder().withNewDefaultMetadata()
                .endMetadata()
                .withBuildId("AABBCC")
                .withStatus(SbomGenerationStatus.NEW)
                .build();

        // ConfigMap cm = kubernetesClient.configMaps().resource(req).create();

        // System.out.println(cm);
    }
}
