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
package org.jboss.sbomer.service.feature.sbom.k8s.reconciler;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.LeaderElectionConfiguration;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Produces a new instance of {@link LeaderElectionConfiguration} configured for SBOMer.
 *
 * @author Marek Goldmann
 */
@ApplicationScoped
@Slf4j
public class SbomerLeaderElectionConfigurationProducer {
    @Inject
    KubernetesClient kubernetesClient;

    @Produces
    @ApplicationScoped
    public LeaderElectionConfiguration getLeaderElectionConfiguration() {
        String hostname = System.getenv("HOSTNAME");
        String namespace = kubernetesClient.getNamespace();

        log.info(
                "Preparing new leader election configuration for SBOMer in namespace {} and {} identity",
                namespace,
                hostname);

        return new LeaderElectionConfiguration("sbomer", namespace, hostname);
    }
}
