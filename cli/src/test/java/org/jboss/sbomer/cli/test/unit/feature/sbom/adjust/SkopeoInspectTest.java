package org.jboss.sbomer.cli.test.unit.feature.sbom.adjust;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.jboss.sbomer.cli.feature.sbom.adjuster.ContainerImageInspectOutput;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Test;

class SkopeoInspectTest {
    @Test
    void parseSkopeoOutput() throws IOException {
        String rawData = TestResources.asString("skopeo.json");

        ContainerImageInspectOutput data = ObjectMapperProvider.json()
                .readValue(rawData, ContainerImageInspectOutput.class);

        assertEquals("sha256:f63b27a29c032843941b15567ebd1f37f540160e8066ac74c05367134c2ff3aa", data.getDigest());
        assertEquals("registry.redhat.io/jboss-webserver-5/jws58-openjdk17-openshift-rhel8", data.getName());
        assertEquals("amd64", data.getArchitecture());
        assertEquals(34, data.getLabels().size());
    }
}
