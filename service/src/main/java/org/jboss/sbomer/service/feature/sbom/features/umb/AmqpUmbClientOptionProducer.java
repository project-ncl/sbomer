/*
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
package org.jboss.sbomer.service.feature.sbom.features.umb;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.sbomer.core.errors.ApplicationException;

import io.smallrye.common.annotation.Identifier;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.core.net.PfxOptions;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Marek Goldmann
 */
@Singleton
@Slf4j
public class AmqpUmbClientOptionProducer {

    @Produces
    @Identifier("umb")
    public AmqpClientOptions getClientOptions() {
        log.info("Setting up AMQP client options");

        String path = System.getenv("SBOMER_KEYSTORE_PATH");
        String password = System.getenv("SBOMER_KEYSTORE_PASSWORD");

        if (path == null || password == null) {
            throw new ApplicationException(
                    "The path or password to keystore was not provided, please make sure you set up the SBOMER_KEYSTORE_PATH and SBOMER_KEYSTORE_PASSWORD environment variables correctly");
        }

        if (Files.notExists(Path.of(path))) {
            throw new ApplicationException(
                    "The keystore file path provided by the SBOMER_KEYSTORE_PATH environment variable does not exist: '{}'",
                    path);
        }

        log.debug("Using '{}' keystore to read certificates to connect to AMQP broker", path);

        return new AmqpClientOptions().setSsl(true)
                .setConnectTimeout(30 * 1000)
                .setReconnectInterval(5 * 1000)
                .setPfxKeyCertOptions(new PfxOptions().setPath(path).setPassword(password));
    }
}
