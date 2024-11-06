package org.jboss.sbomer.service.test.utils.umb;

import java.util.List;
import java.util.Set;

import org.jboss.sbomer.service.test.utils.AlternativeRequestEventRepository;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TestUmbProfile implements QuarkusTestProfile {
    @Override
    public Set<Class<?>> getEnabledAlternatives() {
        return Set.of(TestAmqpUmbClientOptionProducer.class, AlternativeRequestEventRepository.class);
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(AmqpTestResourceLifecycleManager.class));
    }
}