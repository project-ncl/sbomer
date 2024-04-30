package org.jboss.sbomer.service.test.utils;

import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;

import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@Mock
public class MockedGenerationRequestControllerConfig implements GenerationRequestControllerConfig {

    @Override
    public String sbomDir() {
        throw new UnsupportedOperationException("Unimplemented method 'sbomDir'");
    }

    @Override
    public boolean cleanup() {
        throw new UnsupportedOperationException("Unimplemented method 'cleanup'");
    }

}