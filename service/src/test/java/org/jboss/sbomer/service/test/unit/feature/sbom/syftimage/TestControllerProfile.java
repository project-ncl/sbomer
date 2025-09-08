package org.jboss.sbomer.service.test.unit.feature.sbom.syftimage;

import java.util.Map;
import java.util.Set;

import org.jboss.sbomer.service.test.utils.AlternativeGeneratorConfigProvider;
import org.jboss.sbomer.service.test.utils.AlternativeRequestEventRepository;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TestControllerProfile implements QuarkusTestProfile {
    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(AlternativeRequestEventRepository.class, AlternativeGeneratorConfigProvider.class);
    }

    // Override fault tolerance and logging
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "Retry/maxRetries",
                "2",
                "Retry/delay",
                "200",
                "Retry/delayUnit",
                "millis",
                "FibonacciBackoff/maxDelay",
                "6830",
                "ExponentialBackoff/maxDelay",
                "4000",
                "Retry/maxDelayUnit",
                "millis",
                "Retry/maxDurationUnit",
                "millis",
                "Timeout/unit",
                "millis",
                "quarkus.log.console.enable",
                "false",
                "quarkus.log.file.enable",
                "false");
    }
}
