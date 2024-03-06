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
package org.jboss.sbomer.service.feature.sbom.features.umb.consumer;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.pnc.api.enums.BuildStatus;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.api.enums.ProgressStatus;
import org.jboss.pnc.common.Strings;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.PncBuildNotificationMessageBody;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.PncDelAnalysisNotificationMessageBody;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequestBuilder;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Handler for PNC build notifications.
 *
 * @author Marek Goldmann
 */
@ApplicationScoped
@Slf4j
public class PncNotificationHandler {

    @Inject
    UmbConfig config;

    @Inject
    KubernetesClient kubernetesClient;

    public void handle(Message<String> message, GenerationRequestType type) throws JsonProcessingException {
        switch (type) {
            case BUILD:
                PncBuildNotificationMessageBody buildMsgBody = ObjectMapperProvider.json()
                        .readValue(message.getPayload(), PncBuildNotificationMessageBody.class);
                handle(buildMsgBody);
                break;

            case OPERATION:
                PncDelAnalysisNotificationMessageBody delAnalysisMsgBody = ObjectMapperProvider.json()
                        .readValue(message.getPayload(), PncDelAnalysisNotificationMessageBody.class);
                handle(delAnalysisMsgBody);
                break;
            default:
                break;
        }
    }

    /**
     * Handles a particular message received from PNC after the build is finished.
     *
     * @param messageBody the body of the PNC build notification.
     */
    private void handle(PncBuildNotificationMessageBody messageBody) {
        if (messageBody == null) {
            log.warn("Received message does not contain body, ignoring");
            return;
        }

        log.debug("Message of type 'BuildStateChange' properly deserialized");

        if (Strings.isEmpty(messageBody.getBuild().getId())) {
            log.warn("Received message without PNC Build ID specified");
            return;
        }

        if (isSuccessfulPersistentBuild(messageBody)) {
            log.info("Triggering automated SBOM generation for PNC build '{}'' ...", messageBody.getBuild().getId());

            GenerationRequest req = new GenerationRequestBuilder()
                    .withNewDefaultMetadata(messageBody.getBuild().getId(), GenerationRequestType.BUILD)
                    .endMetadata()
                    .withIdentifier(messageBody.getBuild().getId())
                    .withType(GenerationRequestType.BUILD)
                    .withStatus(SbomGenerationStatus.NEW)
                    .build();

            log.debug("ConfigMap to create: '{}'", req);

            ConfigMap cm = kubernetesClient.configMaps().resource(req).create();

            log.info("Request created: {}", cm.getMetadata().getName());
        }
    }

    /**
     * Handles a particular message received from PNC after the deliverable analysis is finished.
     *
     * @param messageBody the body of the PNC build notification.
     */
    private void handle(PncDelAnalysisNotificationMessageBody messageBody) {
        if (messageBody == null) {
            log.warn("Received message does not contain body, ignoring");
            return;
        }

        log.debug("Message of type 'DeliverableAnalysisStateChange' properly deserialized");

        if (Strings.isEmpty(messageBody.getOperationId())) {
            log.warn("Received message without PNC Operation ID specified");
            return;
        }

        if (isFinishedAnalysis(messageBody)) {

            log.info("Triggering automated SBOM generation for PNC build '{}'' ...", messageBody.getOperationId());
            GenerationRequest req = new GenerationRequestBuilder()
                    .withNewDefaultMetadata(messageBody.getOperationId(), GenerationRequestType.OPERATION)
                    .endMetadata()
                    .withIdentifier(messageBody.getOperationId())
                    .withType(GenerationRequestType.OPERATION)
                    .withStatus(SbomGenerationStatus.NEW)
                    .build();

            log.debug("ConfigMap to create: '{}'", req);

            ConfigMap cm = kubernetesClient.configMaps().resource(req).create();

            log.info("Request created: {}", cm.getMetadata().getName());
        }
    }

    private boolean isSuccessfulPersistentBuild(PncBuildNotificationMessageBody msgBody) {
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
                && (BuildType.MVN.equals(msgBody.getBuild().getBuildConfigRevision().getBuildType())
                        || BuildType.GRADLE.equals(msgBody.getBuild().getBuildConfigRevision().getBuildType())
                        || BuildType.NPM.equals(msgBody.getBuild().getBuildConfigRevision().getBuildType()))) {
            return true;
        }

        return false;
    }

    private boolean isFinishedAnalysis(PncDelAnalysisNotificationMessageBody msgBody) {
        log.info(
                "Received UMB message notification operation {}, with status {} and deliverable urls {}",
                msgBody.getOperationId(),
                msgBody.getStatus(),
                String.join(";", msgBody.getDeliverablesUrls()));

        if (ProgressStatus.FINISHED.equals(msgBody.getStatus())) {
            return true;
        }

        return false;
    }
}
