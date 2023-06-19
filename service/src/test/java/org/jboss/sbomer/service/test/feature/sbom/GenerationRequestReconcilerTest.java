package org.jboss.sbomer.service.test.feature.sbom;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.List;

import javax.inject.Inject;

import org.awaitility.Awaitility;
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
public class GenerationRequestReconcilerTest {
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

        GenerationRequest request = new GenerationRequestBuilder().withNewMetadata()
                .withName("test")
                .endMetadata()
                .withBuildId("AABBCC")
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
