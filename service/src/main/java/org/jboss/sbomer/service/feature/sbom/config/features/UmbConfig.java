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
package org.jboss.sbomer.service.feature.sbom.config.features;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * @author Marek Goldmann
 */
@ApplicationScoped
@ConfigMapping(prefix = "sbomer.features.umb")
public interface UmbConfig {

    enum UmbConsumerTrigger {
        NONE, ALL, PRODUCT
    }

    interface UmbConsumerConfig {
        /**
         * Enables the UMB consumer feature
         *
         * @return {@code true} if enabled, {@code false} otherwise
         */
        @WithDefault("false")
        @WithName("enabled")
        boolean isEnabled();

        /**
         * The topic that should be listened on.
         */
        Optional<String> topic();

        Optional<UmbConsumerTrigger> trigger();
    }

    interface UmbProducerConfig {
        /**
         * Enables the UMB producer feature
         *
         * @return {@code true} if enabled, {@code false} otherwise
         */
        @WithDefault("false")
        @WithName("enabled")
        boolean isEnabled();

        /**
         * The topic that should be used to send messages to.
         */
        Optional<String> topic();

        /**
         * The number of retries when sending notification via UMB
         *
         */
        @WithDefault("15")
        int retries();

        /**
         * The maximum number of seconds to back-off the retry
         *
         */
        @WithDefault("30")
        int maxBackOff();

    }

    @WithDefault("false")
    @WithName("enabled")
    boolean isEnabled();

    UmbConsumerConfig consumer();

    UmbProducerConfig producer();
}
