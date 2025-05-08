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
package org.jboss.sbomer.service.scheduler;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@ConfigMapping(prefix = "sbomer.service.generation-scheduler")
public interface GenerationSchedulerConfig {
    /**
     * Defines how many requests can be handled concurrently within the namespace. If the capacity is lower than what we
     * have in the DB -- the ones in DB will need to wait.
     */
    @WithDefault("20")
    int maxConcurrentGenerations();

    /**
     * Maximum number of generations that will fetched from the database to be scheduled within the namespace.
     */
    @WithDefault("10")
    int syncBatch();

    /**
     * The interval on which the scheduler will run.
     */
    @WithDefault("15s")
    String syncInterval();
}
