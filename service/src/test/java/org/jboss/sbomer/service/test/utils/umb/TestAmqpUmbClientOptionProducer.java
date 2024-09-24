package org.jboss.sbomer.service.test.utils.umb;

import org.jboss.sbomer.service.feature.sbom.features.umb.AmqpUmbClientOptionProducer;

import io.smallrye.common.annotation.Identifier;
import io.vertx.amqp.AmqpClientOptions;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Alternative
@Singleton
@Slf4j
public class TestAmqpUmbClientOptionProducer extends AmqpUmbClientOptionProducer {
    @Override
    @Produces
    @Identifier("umb")
    public AmqpClientOptions getClientOptions() {
        log.info("Returning default AmqpClientOptions...");
        return new AmqpClientOptions();
    }
}