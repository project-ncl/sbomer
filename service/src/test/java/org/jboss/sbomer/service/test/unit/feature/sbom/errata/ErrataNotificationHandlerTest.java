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
package org.jboss.sbomer.service.test.unit.feature.sbom.errata;

import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_CONSUMER;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG_STATUS;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG;
import static org.jboss.sbomer.service.feature.sbom.model.RequestEvent.EVENT_KEY_UMB_MSG_TYPE;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.argThat;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.jboss.pnc.build.finder.koji.ClientSession;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.ErrataNotificationHandler;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.RequestEventType;
import org.jboss.sbomer.service.feature.sbom.service.AdvisoryService;
import org.jboss.sbomer.service.test.utils.QuarkusTransactionalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.redhat.red.build.koji.KojiClientException;
import com.redhat.red.build.koji.model.xmlrpc.KojiBuildInfo;

@QuarkusTransactionalTest
class ErrataNotificationHandlerTest {

    static class ErrataNotificationHandlerAlt extends ErrataNotificationHandler {
        public void handle(RequestEvent requestEvent) throws JsonProcessingException, IOException {
            super.handle(requestEvent);
        }
    }

    ErrataNotificationHandlerAlt errataNotificationHandler;

    ErrataClient errataClient = mock(ErrataClient.class);
    ClientSession clientSession = mock(ClientSession.class);
    AdvisoryService advisoryService = mock(AdvisoryService.class);

    @BeforeEach
    void beforeEach() {
        errataNotificationHandler = new ErrataNotificationHandlerAlt();

        FeatureFlags featureFlags = mock(FeatureFlags.class);
        when(featureFlags.errataIntegrationEnabled()).thenReturn(true);
        when(featureFlags.standardErrataRPMManifestGenerationEnabled()).thenReturn(true);
        when(featureFlags.standardErrataImageManifestGenerationEnabled()).thenReturn(true);
        when(featureFlags.textOnlyErrataManifestGenerationEnabled()).thenReturn(true);
        errataNotificationHandler.setFeatureFlags(featureFlags);
        errataNotificationHandler.setAdvisoryService(advisoryService);
        advisoryService.setErrataClient(errataClient);
    }

    @Test
    void testHandleRealErrataWithNewFilesStatus() throws IOException, JsonProcessingException {
        String errataJsonString = TestResources.asString("errata/api/erratum.json");
        String releaseJsonString = TestResources.asString("errata/api/release.json");
        String errataBuildsJsonString = TestResources.asString("errata/api/build_list.json");
        Errata errata = ObjectMapperProvider.json().readValue(errataJsonString, Errata.class);
        ErrataRelease release = ObjectMapperProvider.json().readValue(releaseJsonString, ErrataRelease.class);
        ErrataBuildList buildList = ObjectMapperProvider.json()
                .readValue(errataBuildsJsonString, ErrataBuildList.class);

        String umbErrataStatusChangeMsg = TestResources.asString("errata/umb/errata_status_change.json");

        ObjectNode event = ObjectMapperProvider.json().createObjectNode();
        event.put(EVENT_KEY_UMB_CONSUMER, UMBConsumer.ERRATA.toString());
        event.put(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.NONE.toString());
        event.put(EVENT_KEY_UMB_MSG, umbErrataStatusChangeMsg);
        event.put(EVENT_KEY_UMB_MSG_TYPE, ErrataAdvisoryRequestConfig.TYPE_NAME);

        RequestConfig requestConfig = ErrataAdvisoryRequestConfig.builder().withAdvisoryId("139230").build();

        // Create the initial requestEvent
        RequestEvent requestEvent = RequestEvent.createNew(requestConfig, RequestEventType.UMB, event).save();

        when(errataClient.getErratum("139230")).thenReturn(errata);
        when(errataClient.getBuildsList("139230")).thenReturn(buildList);
        when(errataClient.getRelease("2227")).thenReturn(release);

        errataNotificationHandler.handle(requestEvent);
    }

    @Test
    void testHandleRealErrataDockerWithQEStatus() throws KojiClientException, IOException, JsonProcessingException {
        String errataJsonString = TestResources.asString("errata/api/erratum_QE.json");
        String releaseJsonString = TestResources.asString("errata/api/erratum_QE_release.json");
        String errataBuildsJsonString = TestResources.asString("errata/api/erratum_QE_build_list.json");
        Errata errata = ObjectMapperProvider.json().readValue(errataJsonString, Errata.class);
        ErrataRelease release = ObjectMapperProvider.json().readValue(releaseJsonString, ErrataRelease.class);
        ErrataBuildList buildList = ObjectMapperProvider.json()
                .readValue(errataBuildsJsonString, ErrataBuildList.class);

        String umbErrataStatusChangeMsg = TestResources.asString("errata/umb/errata_status_change_QE.json");
        KojiBuildInfo kojiBuildInfo = createKojiBuildInfo();

        ObjectNode event = ObjectMapperProvider.json().createObjectNode();
        event.put(EVENT_KEY_UMB_CONSUMER, UMBConsumer.ERRATA.toString());
        event.put(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.NONE.toString());
        event.put(EVENT_KEY_UMB_MSG, umbErrataStatusChangeMsg);
        event.put(EVENT_KEY_UMB_MSG_TYPE, ErrataAdvisoryRequestConfig.TYPE_NAME);

        RequestConfig requestConfig = ErrataAdvisoryRequestConfig.builder().withAdvisoryId("139856").build();

        // Create the initial requestEvent
        RequestEvent requestEvent = RequestEvent.createNew(requestConfig, RequestEventType.UMB, event).save();

        when(errataClient.getErratum("139856")).thenReturn(errata);
        when(errataClient.getBuildsList("139856")).thenReturn(buildList);
        when(errataClient.getRelease("2096")).thenReturn(release);
        when(clientSession.getBuild(3338841)).thenReturn(kojiBuildInfo);

        errataNotificationHandler.handle(requestEvent);

        ArgumentMatcher<RequestEvent> hasAdvisoryId = cfg -> cfg != null
                && "139856".equals(((ErrataAdvisoryRequestConfig) cfg.getRequestConfig()).getAdvisoryId());

        verify(advisoryService, times(1)).generateFromAdvisory(argThat(hasAdvisoryId));
    }

    private KojiBuildInfo createKojiBuildInfo() {
        KojiBuildInfo kojiBuildInfo = new KojiBuildInfo();
        kojiBuildInfo.setName("podman-container");
        kojiBuildInfo.setVersion("9.4");
        kojiBuildInfo.setRelease("14.1728871566");
        Map<String, Object> extras = Map.of(
                "image",
                Map.of(
                        "index",
                        Map.of(
                                "pull",
                                List.of(
                                        "registry-proxy.com/rh-osbs/rhel9-podman@sha256:a9a84a89352ab1cbe3f5b094b4abbc7c5800edf65f5d52751932bd6488433d63",
                                        "registry-proxy.com/rh-osbs/rhel9-podman:9.4-14.1728871566"))));

        kojiBuildInfo.setExtra(extras);
        return kojiBuildInfo;
    }

}
