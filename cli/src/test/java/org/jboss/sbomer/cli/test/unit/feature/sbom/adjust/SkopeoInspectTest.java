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

        assertEquals("sha256:ee4e27734a21cc6b8a8597ef2af32822ad0b4677dbde0a794509f55cbaff5ab3", data.getDigest());
        assertEquals("registry.com/rh-osbs/amq-streams-console-ui-rhel9", data.getName());
        assertEquals("amd64", data.getArchitecture());
        assertEquals(21, data.getLabels().size());
    }
}
