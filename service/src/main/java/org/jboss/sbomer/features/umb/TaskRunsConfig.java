/**
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
package org.jboss.sbomer.features.umb;

import javax.enterprise.context.ApplicationScoped;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * @author Andrea Vibelli
 */
@ApplicationScoped
@ConfigMapping(prefix = "sbomer.features.taskruns")
public interface TaskRunsConfig {

    /**
     * Enables the cleanup of successful taskruns
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    @WithDefault("true")
    @WithName("cleanup-successful")
    boolean cleanupSuccessful();

    interface RetriesConfig {
        /**
         * Enables the retries in case of failed taskruns
         *
         * @return {@code true} if enabled, {@code false} otherwise
         */
        @WithDefault("true")
        @WithName("enabled")
        boolean isEnabled();

        /**
         * The maximum number of retries to attempt for failed taskruns
         *
         */
        @WithDefault("10")
        @WithName("max-retries")
        int maxRetries();

    }

    RetriesConfig retries();
}
