package org.jboss.sbomer.service.test.integ.feature.sbom.messaging;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

public class AmqpTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {
    @Override
    public Map<String, String> start() {
        Map<String, String> env = new HashMap<>();
        Map<String, String> buildsProps = InMemoryConnector.switchIncomingChannelsToInMemory("builds");
        Map<String, String> finishedProps = InMemoryConnector.switchOutgoingChannelsToInMemory("finished");
        env.putAll(buildsProps);
        env.putAll(finishedProps);
        return env;
    }

    @Override
    public void stop() {
        InMemoryConnector.clear();
    }
}
