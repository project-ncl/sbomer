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
package org.jboss.sbomer.service.leader;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.coordination.v1.Lease;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseBuilder;
import io.fabric8.kubernetes.api.model.coordination.v1.LeaseSpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class LeaderManager {

    private LeaseConfig leaseConfig;
    private KubernetesClient kubernetesClient;

    @ConfigProperty(name = "SBOMER_RELEASE", defaultValue = "sbomer")
    String release = "sbomer";

    @ConfigProperty(name = "HOSTNAME", defaultValue = "sbomer")
    String hostname = "sbomer";

    @Getter
    boolean isLeader = false;

    @Inject
    public LeaderManager(LeaseConfig leaseConfig, KubernetesClient kubernetesClient) {
        this.leaseConfig = leaseConfig;
        this.kubernetesClient = kubernetesClient;
    }

    @Scheduled(
            every = "${sbomer.service.leader.check-interval:10s}",
            delay = 5,
            delayUnit = TimeUnit.SECONDS,
            concurrentExecution = ConcurrentExecution.SKIP)
    public void lease() {
        String leaseName = release + "-generation-scheduler";

        log.info("Reading '{}' lease information...", leaseName);

        Lease lease = kubernetesClient.leases().withName(leaseName).get();

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));

        if (lease != null && lease.getSpec() != null) {
            log.debug("Lease exists");

            LeaseSpec leaseSpec = lease.getSpec();

            String holder = leaseSpec.getHolderIdentity();
            ZonedDateTime renewTime = leaseSpec.getRenewTime();
            Instant expiration = null;

            if (renewTime != null) {
                expiration = renewTime.toInstant().plusSeconds(leaseSpec.getLeaseDurationSeconds());
            }

            if (holder != null && !holder.equals(hostname) && expiration != null
                    && now.toInstant().isBefore(expiration)) {

                log.info("I'm not the leader nor it's time to take over the lead, current leader: {}", holder);

                isLeader = false;

                return;
            }
        }

        log.info("Ensuring I'm the leader by updating lease information in the cluster...");

        lease = new LeaseBuilder().withNewMetadata()
                .withName(leaseName)
                .endMetadata()
                .withNewSpec()
                .withHolderIdentity(hostname)
                .withLeaseDurationSeconds(leaseConfig.leaseDuration())
                .withRenewTime(now)
                .withAcquireTime(now)
                .endSpec()
                .build();

        kubernetesClient.leases().resource(lease).createOrReplace();

        isLeader = true;

        return;
    }

}
