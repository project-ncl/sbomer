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
package org.jboss.sbomer.features.umb.consumer;

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

import org.jboss.pnc.api.enums.BuildStatus;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.api.enums.ProgressStatus;
import org.jboss.pnc.common.Strings;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.sbomer.core.enums.GeneratorImplementation;
import org.jboss.sbomer.core.service.PncService;
import org.jboss.sbomer.core.service.ProductVersionMapper;
import org.jboss.sbomer.core.service.ProductVersionMapper.ProductVersionMapping;
import org.jboss.sbomer.features.umb.JmsUtils;
import org.jboss.sbomer.features.umb.UmbConfig;
import org.jboss.sbomer.features.umb.UmbConfig.UmbConsumerTrigger;
import org.jboss.sbomer.features.umb.consumer.model.PncBuildNotificationMessageBody;
import org.jboss.sbomer.service.GenerationService;

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
    GenerationService generationService;

    @Inject
    PncService pncService;

    @Inject
    UmbConfig config;

    @Inject
    ConnectionFactory cf;

    @Inject
    ProductVersionMapper productVersionMapper;

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
                        // Check whether it is a product-related build
                        if (Objects.equals(config.consumer().trigger(), UmbConsumerTrigger.PRODUCT)) {
                            ProductVersionRef productVersion = pncService.getProductVersion(msgBody.getBuild().getId());

                            if (productVersion == null) {
                                log.warn(
                                        "The UMB consumer configuration is set to PRODUCT, skipping SBOM generation for PNC Build '{}' because it is not related to a Product",
                                        msgBody.getBuild().getId());
                                continue;
                            }

                            ProductVersionMapping mapping = productVersionMapper.getMapping()
                                    .get(productVersion.getId());

                            if (mapping == null) {
                                log.warn(
                                        "Could not find mapping for the PNC Product Version '{}' (id: {}), skipping SBOM generation",
                                        productVersion.getVersion(),
                                        productVersion.getId());
                                continue;
                            }

                            // TODO: Milestone 1
                            // We support only generation of products that we list in the product mapping.

                            // Use the generator in the mapping if specified, otherwise ude the default (CYCLONEDX)
                            GeneratorImplementation generator = GeneratorImplementation.CYCLONEDX;
                            try {
                                generator = GeneratorImplementation.valueOf(mapping.getGenerator());
                            } catch (IllegalArgumentException exc) {
                            }

                            log.info(
                                    "Detected {} as the required generator from the PNC Product Version mapping",
                                    generator);

                            switch (generator) {
                                case DOMINO: {
                                    generationService.generate(
                                            msgBody.getBuild().getId(),
                                            GeneratorImplementation.DOMINO,
                                            "0.0.88",
                                            "--include-non-managed --exclude-parent-poms --warn-on-missing-scm");
                                    break;
                                }
                                default: {
                                    generationService
                                            .generate(msgBody.getBuild().getId(), GeneratorImplementation.CYCLONEDX);
                                }
                            }
                        } else {
                            log.info(
                                    "Triggering the automated SBOM generation for build {} ...",
                                    msgBody.getBuild().getId());

                            generationService.generate(msgBody.getBuild().getId(), GeneratorImplementation.CYCLONEDX);
                        }
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
            log.error("Something wrong happened in the PNCMessageParser", e);
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

}
