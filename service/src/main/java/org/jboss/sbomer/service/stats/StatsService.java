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
package org.jboss.sbomer.service.stats;

import java.lang.management.ManagementFactory;
import java.time.Duration;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.AmqpMessageConsumer;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.AmqpMessageProducer;
import org.jboss.sbomer.service.feature.sbom.model.Stats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Deployment;
import org.jboss.sbomer.service.feature.sbom.model.Stats.ErrataConsumer;
import org.jboss.sbomer.service.feature.sbom.model.Stats.GenerationRequestStats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Messaging;
import org.jboss.sbomer.service.feature.sbom.model.Stats.PncConsumer;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Producer;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Resources;
import org.jboss.sbomer.service.feature.sbom.model.Stats.SbomStats;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatsService {
    @Inject
    AmqpMessageConsumer messageConsumer;

    @Inject
    AmqpMessageProducer messageProducer;

    @Inject
    SbomService sbomService;

    @Inject
    UmbConfig umbConfig;

    private long getUptimeMillis() {
        return ManagementFactory.getRuntimeMXBean().getUptime();
    }

    private Resources resources() {
        return Resources.builder().withSboms(sbomStats()).withGenerationRequests(generationRequestStats()).build();
    }

    private SbomStats sbomStats() {
        return SbomStats.builder().withTotal(sbomService.countSboms()).build();
    }

    private GenerationRequestStats generationRequestStats() {
        return GenerationRequestStats.builder()
                .withTotal(sbomService.countSbomGenerationRequests())
                .withInProgress(sbomService.countInProgressSbomGenerationRequests())
                .build();
    }

    private String toUptime(long milliseconds) {
        return Duration.ofMillis(milliseconds)
                .toString()
                .substring(2)
                .replaceAll("(\\d[HMS])(?!$)", "$1 ")
                .toLowerCase();
    }

    public Stats getStats() {
        long uptimeMillis = getUptimeMillis();

        Messaging messaging = null;

        if (umbConfig.isEnabled()) {
            messaging = Messaging.builder()
                    .withPncConsumer(
                            PncConsumer.builder()
                                    .withProcessed(messageConsumer.getPncProcessedMessages())
                                    .withReceived(messageConsumer.getPncReceivedMessages())
                                    .withSkipped(messageConsumer.getPncSkippedMessages())
                                    .build())
                    .withErrataConsumer(
                            ErrataConsumer.builder()
                                    .withProcessed(messageConsumer.getErrataProcessedMessages())
                                    .withReceived(messageConsumer.getErrataReceivedMessages())
                                    .withSkipped(messageConsumer.getErrataSkippedMessages())
                                    .build())

                    .withProducer(
                            Producer.builder()
                                    .withAcked(messageProducer.getAckedMessages())
                                    .withNacked(messageProducer.getNackedMessages())
                                    .build())
                    .build();
        }

        return Stats.builder()
                .withVersion(
                        ConfigProvider.getConfig()
                                .getOptionalValue("quarkus.application.version", String.class)
                                .orElse("dev"))
                .withUptime(toUptime(uptimeMillis))
                .withUptimeMillis(uptimeMillis)
                .withResources(resources())
                .withMessaging(messaging)
                .withRelease(ConfigProvider.getConfig().getOptionalValue("sbomer.release", String.class).orElse("dev"))
                .withAppEnv(ConfigProvider.getConfig().getOptionalValue("app.env", String.class).orElse("dev"))
                .withHostname(ConfigProvider.getConfig().getOptionalValue("hostname", String.class).orElse(null))
                .withDeployment(
                        Deployment.builder()
                                .withTarget(
                                        ConfigProvider.getConfig()
                                                .getOptionalValue("sbomer.deployment.target", String.class)
                                                .orElse("dev"))
                                .withType(
                                        ConfigProvider.getConfig()
                                                .getOptionalValue("sbomer.deployment.type", String.class)
                                                .orElse("dev"))
                                .withZone(
                                        ConfigProvider.getConfig()
                                                .getOptionalValue("sbomer.deployment.zone", String.class)
                                                .orElse("dev"))
                                .build())
                .build();
    }
}
