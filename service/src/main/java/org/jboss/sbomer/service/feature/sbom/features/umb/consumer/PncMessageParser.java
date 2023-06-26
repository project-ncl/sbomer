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
package org.jboss.sbomer.service.feature.sbom.features.umb.consumer;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.qpid.jms.provider.exceptions.ProviderConnectionRemotelyClosedException;
import org.jboss.pnc.api.enums.BuildStatus;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.api.enums.ProgressStatus;
import org.jboss.pnc.common.Strings;
import org.jboss.sbomer.service.feature.sbom.features.umb.JmsUtils;
import org.jboss.sbomer.service.feature.sbom.features.umb.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.UmbConfig.UmbConsumerTrigger;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.PncBuildNotificationMessageBody;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.arc.Unremovable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Unremovable
@ApplicationScoped
public class PncMessageParser implements Runnable {

    private AtomicBoolean shouldRun = new AtomicBoolean(false);
    private AtomicBoolean connected = new AtomicBoolean(false);
    private AtomicInteger receivedMessages = new AtomicInteger(0);

    @Inject
    UmbConfig config;

    @Inject
    ConnectionFactory cf;

    @Inject
    KubernetesClient kubernetesClient;

    private Message lastMessage;

    public PncMessageParser() {
        this.shouldRun.set(true);
    }

    @Override
    public void run() {
        if (!config.isEnabled()) {
            log.warn("The UMB feature is disabled");
            return;
        }

        if (!config.consumer().isEnabled()) {
            log.warn("The UMB consumer is disabled");
            return;
        }
        if (config.consumer().topic().isEmpty()) {
            log.warn("Topic not specified, PNC message parser won't run");
            return;
        }

        if (config.consumer().trigger() == UmbConsumerTrigger.NONE) {
            log.warn("The UMB consumer configuration is set to NONE, all builds are skipped");
        }

        log.info("Listening on topic: {}", config.consumer().topic().get());

        try (JMSContext context = cf.createContext(Session.AUTO_ACKNOWLEDGE)) {
            log.info("JMS client ID {}.", context.getClientID());

            JMSConsumer consumer = context.createConsumer(context.createQueue(config.consumer().topic().get()));
            while (shouldRun.get()) {
                connected.set(true);
                Message message = consumer.receive();
                if (message == null) {
                    // receive returns `null` if the JMSConsumer is closed
                    return;
                }

                lastMessage = message;
                receivedMessages.incrementAndGet();

                try {
                    PncBuildNotificationMessageBody msgBody = JmsUtils.getMsgBody(lastMessage);

                    if (msgBody == null) {
                        continue;
                    }

                    if (Strings.isEmpty(msgBody.getBuild().getId())) {
                        log.warn("Received UMB message without Build ID specified");
                        continue;
                    }

                    if (Objects.equals(config.consumer().trigger(), UmbConsumerTrigger.NONE)) {
                        log.warn(
                                "The UMB consumer configuration is set to NONE, skipping SBOM generation for PNC Build '{}'",
                                msgBody.getBuild().getId());
                        continue;
                    }

                    if (isSuccessfulPersistentBuild(msgBody)) {
                        // TODO: Check whether it is a product-related build?

                        log.info(
                                "Triggering the automated SBOM generation for build {} ...",
                                msgBody.getBuild().getId());

                        GenerationRequest req = new GenerationRequestBuilder()
                                .withNewDefaultMetadata(msgBody.getBuild().getId())
                                .endMetadata()
                                .withBuildId(msgBody.getBuild().getId())
                                .withStatus(SbomGenerationStatus.NEW)
                                .build();

                        ConfigMap cm = kubernetesClient.configMaps().resource(req).create();

                        log.info("Request created: {}", cm.getMetadata().getName());
                    }
                } catch (JMSException | IOException e) {
                    log.error(
                            "Cannot convert UMB message {} from topic {} to Json",
                            message.getJMSMessageID(),
                            message.getJMSDestination(),
                            e);
                }

            }
        } catch (Exception e) {
            Throwable cause = getRootCause(e);
            if (ProviderConnectionRemotelyClosedException.class.equals(cause.getClass())) {
                log.warn("The JMS connection was remotely closed, will be handled with the reconnection.");
            } else {
                log.error("Something wrong happened in the PNCMessageParser", e);
            }
        }

        connected.set(false);
    }

    public boolean shouldRun() {
        return shouldRun.get();
    }

    public void setShouldRun(boolean shouldRun) {
        this.shouldRun.set(shouldRun);
    }

    public boolean isConnected() {
        return connected.get();
    }

    public Message getLastMessage() {
        return lastMessage;
    }

    public int getReceivedMessages() {
        return receivedMessages.get();
    }

    public boolean isSuccessfulPersistentBuild(PncBuildNotificationMessageBody msgBody) {
        log.info(
                "Received UMB message notification for {} build {}, with status {}, progress {} and build type {}",
                msgBody.getBuild().isTemporaryBuild() ? "temporary" : "persistent",
                msgBody.getBuild().getId(),
                msgBody.getBuild().getStatus(),
                msgBody.getBuild().getProgress(),
                msgBody.getBuild().getBuildConfigRevision().getBuildType());

        if (!msgBody.getBuild().isTemporaryBuild() && ProgressStatus.FINISHED.equals(msgBody.getBuild().getProgress())
                && (BuildStatus.SUCCESS.equals(msgBody.getBuild().getStatus())
                        || BuildStatus.NO_REBUILD_REQUIRED.equals(msgBody.getBuild().getStatus()))
                && BuildType.MVN.equals(msgBody.getBuild().getBuildConfigRevision().getBuildType())) {
            return true;
        }

        return false;
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        while (cause != null && cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause;
    }

}
