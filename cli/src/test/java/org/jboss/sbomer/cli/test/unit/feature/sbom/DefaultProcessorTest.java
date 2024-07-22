package org.jboss.sbomer.cli.test.unit.feature.sbom;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.feature.sbom.processor.DefaultProcessor;
import org.jboss.sbomer.cli.feature.sbom.service.KojiService;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.pnc.PncService;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class DefaultProcessorTest {

    @Test
    void testProcessingWhenNoDataIsAvailable() throws IOException {
        PncService pncServiceMock = Mockito.mock(PncService.class);
        KojiService kojiServiceMock = Mockito.mock(KojiService.class);

        DefaultProcessor defaultProcessor = new DefaultProcessor(pncServiceMock, kojiServiceMock);

        Bom bom = SbomUtils.fromString(TestResources.asString("boms/image-after-adjustments.json"));
        Bom processed = defaultProcessor.process(bom);

        assertEquals(143, processed.getComponents().size());
    }
}
