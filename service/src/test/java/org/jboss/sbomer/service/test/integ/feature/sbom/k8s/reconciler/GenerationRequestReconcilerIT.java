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
package org.jboss.sbomer.service.test.integ.feature.sbom.k8s.reconciler;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;

import jakarta.inject.Inject;

import org.awaitility.Awaitility;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@Disabled("This doesn't work, because we are not starting Tekton properly, we need to think about different ways of running integration tests")
public class GenerationRequestReconcilerIT {
    @Inject
    Operator operator;

    @Inject
    KubernetesClient client;

    @BeforeEach
    void beforeEach() throws Exception {
        var tektonRelease = "https://github.com/tektoncd/pipeline/releases/download/v0.41.1/release.yaml";
        List<HasMetadata> result = client.load(new URL(tektonRelease).openStream()).get();
        client.resourceList(result).createOrReplace();
    }

    @Test
    void testReconciler() {
        operator.start();

        GenerationRequest request = new GenerationRequestBuilder()
                .withNewDefaultMetadata("AABBCC", GenerationRequestType.BUILD)
                .withName("test")
                .endMetadata()
                .withIdentifier("AABBCC")
                .withType(GenerationRequestType.BUILD)
                .build();

        GenerationRequest created = client.resource(request).create();

        Awaitility.await().ignoreException(NullPointerException.class).atMost(300, SECONDS).untilAsserted(() -> {
            GenerationRequest updatedRequest = client.resources(GenerationRequest.class)
                    .inNamespace(created.getMetadata().getNamespace())
                    .withName(created.getMetadata().getName())
                    .get();

            assertEquals(SbomGenerationStatus.INITIALIZING, updatedRequest.getStatus());
        });

    }
}
