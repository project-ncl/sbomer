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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.ArgumentMatchers.argThat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.pnc.build.finder.koji.ClientSession;
import org.jboss.pnc.dto.DeliverableAnalyzerOperation;
import org.jboss.sbomer.core.config.request.ErrataAdvisoryRequestConfig;
import org.jboss.sbomer.core.config.request.RequestConfig;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.core.features.sbom.config.DeliverableAnalysisConfig;
import org.jboss.sbomer.core.features.sbom.config.OperationConfig;
import org.jboss.sbomer.core.features.sbom.config.PncBuildConfig;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.enums.RequestEventType;
import org.jboss.sbomer.core.features.sbom.enums.UMBConsumer;
import org.jboss.sbomer.core.features.sbom.enums.UMBMessageStatus;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataNotesSchemaValidator;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.ErrataNotificationHandler;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.AdvisoryService;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.test.utils.QuarkusTransactionalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
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
    ErrataNotesSchemaValidator notesSchemaValidator;

    ErrataClient errataClient = mock(ErrataClient.class);
    ClientSession clientSession = mock(ClientSession.class);
    AdvisoryService advisoryService = mock(AdvisoryService.class);
    SbomService sbomService = mock(SbomService.class);
    RequestEventRepository requestEventRepository = mock(RequestEventRepository.class);

    @BeforeEach
    void beforeEach() {
        errataNotificationHandler = new ErrataNotificationHandlerAlt();
        notesSchemaValidator = new ErrataNotesSchemaValidator();

        FeatureFlags featureFlags = mock(FeatureFlags.class);
        when(featureFlags.errataIntegrationEnabled()).thenReturn(true);
        when(featureFlags.standardErrataRPMManifestGenerationEnabled()).thenReturn(true);
        when(featureFlags.standardErrataImageManifestGenerationEnabled()).thenReturn(true);
        when(featureFlags.textOnlyErrataManifestGenerationEnabled()).thenReturn(true);
        errataNotificationHandler.setFeatureFlags(featureFlags);
        errataNotificationHandler.setAdvisoryService(advisoryService);
        errataNotificationHandler.setRequestEventRepository(requestEventRepository);
        advisoryService.setErrataClient(errataClient);
        advisoryService.setSbomService(sbomService);
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

        // Create the initial requestEvent
        RequestEvent requestEvent = RequestEvent.createNew(null, RequestEventType.UMB, event).save();

        RequestConfig requestConfig = ErrataAdvisoryRequestConfig.builder().withAdvisoryId("139856").build();
        RequestEvent initializedRequestEvent = RequestEvent.createNew(requestConfig, RequestEventType.UMB, event);
        initializedRequestEvent.setId(requestEvent.getId());

        when(errataClient.getErratum("139856")).thenReturn(errata);
        when(errataClient.getBuildsList("139856")).thenReturn(buildList);
        when(errataClient.getRelease("2096")).thenReturn(release);
        when(clientSession.getBuild(3338841)).thenReturn(kojiBuildInfo);
        when(requestEventRepository.updateRequestConfig(requestEvent, requestConfig))
                .thenReturn(initializedRequestEvent);

        errataNotificationHandler.handle(requestEvent);

        ArgumentMatcher<RequestEvent> hasAdvisoryId = cfg -> cfg != null
                && "139856".equals(((ErrataAdvisoryRequestConfig) cfg.getRequestConfig()).getAdvisoryId());

        verify(advisoryService, times(1)).generateFromAdvisory(argThat(hasAdvisoryId));
    }

    @Test
    void testHandleTextOnlyErrataWithManifestWithQEStatus()
            throws KojiClientException, IOException, JsonProcessingException {
        String textOnlyErrataJsonString = TestResources.asString("errata/api/erratum_textonly_QE_manifest.json");
        String releaseJsonString = TestResources.asString("errata/api/erratum_textonly_QE_release.json");
        String errataBuildsJsonString = TestResources.asString("errata/api/erratum_textonly_QE_build_list.json");
        Errata errata = ObjectMapperProvider.json().readValue(textOnlyErrataJsonString, Errata.class);
        ErrataRelease release = ObjectMapperProvider.json().readValue(releaseJsonString, ErrataRelease.class);
        ErrataBuildList buildList = ObjectMapperProvider.json()
                .readValue(errataBuildsJsonString, ErrataBuildList.class);

        String umbErrataStatusChangeMsg = TestResources.asString("errata/umb/errata_textonly_status_change_QE.json");

        ObjectNode event = ObjectMapperProvider.json().createObjectNode();
        event.put(EVENT_KEY_UMB_CONSUMER, UMBConsumer.ERRATA.toString());
        event.put(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.NONE.toString());
        event.put(EVENT_KEY_UMB_MSG, umbErrataStatusChangeMsg);
        event.put(EVENT_KEY_UMB_MSG_TYPE, ErrataAdvisoryRequestConfig.TYPE_NAME);

        // Create the initial requestEvent
        RequestEvent requestEvent = RequestEvent.createNew(null, RequestEventType.UMB, event).save();

        RequestConfig requestConfig = ErrataAdvisoryRequestConfig.builder().withAdvisoryId("140268").build();
        RequestEvent initializedRequestEvent = RequestEvent.createNew(requestConfig, RequestEventType.UMB, event);
        initializedRequestEvent.setId(requestEvent.getId());

        ArgumentMatcher<RequestEvent> hasAdvisoryId = cfg -> cfg != null
                && "140268".equals(((ErrataAdvisoryRequestConfig) cfg.getRequestConfig()).getAdvisoryId());

        // The notes are valid
        ValidationResult result = notesSchemaValidator.validate(errata);
        assertTrue(result.isValid());

        JsonNode notes = errata.getNotesMapping().get();
        assertNotNull(notes.get("manifest"));

        // It's a manifest so nothing should be done by SBOMer
        Collection<SbomGenerationRequest> expectedResult = Collections.emptyList();

        when(errataClient.getErratum("140268")).thenReturn(errata);
        when(errataClient.getBuildsList("140268")).thenReturn(buildList);
        when(errataClient.getRelease("1245")).thenReturn(release);
        when(requestEventRepository.updateRequestConfig(requestEvent, requestConfig))
                .thenReturn(initializedRequestEvent);
        when(advisoryService.generateFromAdvisory(argThat(hasAdvisoryId))).thenReturn(expectedResult);

        errataNotificationHandler.handle(requestEvent);

        verify(advisoryService, times(1)).generateFromAdvisory(argThat(hasAdvisoryId));
        Collection<SbomGenerationRequest> actualResult = advisoryService.generateFromAdvisory(initializedRequestEvent);
        assertEquals(expectedResult, actualResult);
    }

    @Test
    void testHandleTextOnlyErrataWithDeliverablesWithQEStatus()
            throws KojiClientException, IOException, JsonProcessingException {
        String textOnlyErrataJsonString = TestResources.asString("errata/api/erratum_textonly_QE_deliverables.json");
        String releaseJsonString = TestResources.asString("errata/api/erratum_textonly_QE_release.json");
        String errataBuildsJsonString = TestResources.asString("errata/api/erratum_textonly_QE_build_list.json");
        Errata errata = ObjectMapperProvider.json().readValue(textOnlyErrataJsonString, Errata.class);
        ErrataRelease release = ObjectMapperProvider.json().readValue(releaseJsonString, ErrataRelease.class);
        ErrataBuildList buildList = ObjectMapperProvider.json()
                .readValue(errataBuildsJsonString, ErrataBuildList.class);
        String umbErrataStatusChangeMsg = TestResources.asString("errata/umb/errata_textonly_status_change_QE.json");
        DeliverableAnalysisConfig config = DeliverableAnalysisConfig.builder()
                .withMilestoneId("1234")
                .withDeliverableUrls(List.of("http://host.com/paht/to/first.zip"))
                .build();
        DeliverableAnalyzerOperation operation = DeliverableAnalyzerOperation.delAnalyzerBuilder()
                .id("GFEDCBA")
                .build();

        ObjectNode event = ObjectMapperProvider.json().createObjectNode();
        event.put(EVENT_KEY_UMB_CONSUMER, UMBConsumer.ERRATA.toString());
        event.put(EVENT_KEY_UMB_MSG_STATUS, UMBMessageStatus.NONE.toString());
        event.put(EVENT_KEY_UMB_MSG, umbErrataStatusChangeMsg);
        event.put(EVENT_KEY_UMB_MSG_TYPE, ErrataAdvisoryRequestConfig.TYPE_NAME);

        // Create the initial requestEvent
        RequestEvent requestEvent = RequestEvent.createNew(null, RequestEventType.UMB, event).save();

        RequestConfig requestConfig = ErrataAdvisoryRequestConfig.builder().withAdvisoryId("140268").build();
        RequestEvent initializedRequestEvent = RequestEvent.createNew(requestConfig, RequestEventType.UMB, event);
        initializedRequestEvent.setId(requestEvent.getId());

        ArgumentMatcher<RequestEvent> hasAdvisoryId = cfg -> cfg != null
                && "140268".equals(((ErrataAdvisoryRequestConfig) cfg.getRequestConfig()).getAdvisoryId());

        // The notes are valid
        ValidationResult result = notesSchemaValidator.validate(errata);
        assertTrue(result.isValid());

        JsonNode notes = errata.getNotesMapping().get();
        assertNull(notes.get("manifest"));
        assertNotNull(notes.get("deliverables"));
        assertTrue(notes.get("deliverables").isArray());
        assertEquals(3, notes.get("deliverables").size());

        when(errataClient.getErratum("140268")).thenReturn(errata);
        when(errataClient.getBuildsList("140268")).thenReturn(buildList);
        when(errataClient.getRelease("1245")).thenReturn(release);
        when(sbomService.doAnalyzeDeliverables(config)).thenReturn(operation);
        when(requestEventRepository.updateRequestConfig(requestEvent, requestConfig))
                .thenReturn(initializedRequestEvent);

        Collection<SbomGenerationRequest> expectedResult = new ArrayList<SbomGenerationRequest>();
        expectedResult.add(expectPncBuildSbomGenerationRequest("ABCD"));
        expectedResult.add(expectPncOperationSbomGenerationRequest("1234"));
        expectedResult
                .add(expectPncAnalysisSbomGenerationRequest("GFEDCBA", List.of("http://host.com/paht/to/first.zip")));

        when(advisoryService.generateFromAdvisory(argThat(hasAdvisoryId))).thenReturn(expectedResult);

        errataNotificationHandler.handle(requestEvent);

        verify(advisoryService, times(1)).generateFromAdvisory(argThat(hasAdvisoryId));
        Collection<SbomGenerationRequest> actualResult = advisoryService.generateFromAdvisory(initializedRequestEvent);

        assertEquals(expectedResult.size(), actualResult.size());

        Set<List<Object>> fieldsExpectedSet = expectedResult.stream()
                .map(
                        request -> List.of(
                                request.getIdentifier(),
                                request.getType(),
                                request.getStatus(),
                                request.getConfig()))
                .collect(Collectors.toSet());

        Set<List<Object>> fieldsResultSet = actualResult.stream()
                .map(
                        request -> List.of(
                                request.getIdentifier(),
                                request.getType(),
                                request.getStatus(),
                                request.getConfig()))
                .collect(Collectors.toSet());

        // Check that both sets are equal
        assertEquals(
                fieldsExpectedSet,
                fieldsResultSet,
                "The collections do not contain the same elements based on the specified fields.");
    }

    @Test
    void testHandleErrataWithUnknownContentType() throws KojiClientException, IOException, JsonProcessingException {

        String errataJsonString = TestResources.asString("errata/api/erratum_unknown_content_type.json");
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

        // Create the initial requestEvent
        RequestEvent requestEvent = RequestEvent.createNew(null, RequestEventType.UMB, event).save();

        RequestConfig requestConfig = ErrataAdvisoryRequestConfig.builder().withAdvisoryId("139856").build();
        RequestEvent initializedRequestEvent = RequestEvent.createNew(requestConfig, RequestEventType.UMB, event);
        initializedRequestEvent.setId(requestEvent.getId());

        when(errataClient.getErratum("139856")).thenReturn(errata);
        when(errataClient.getBuildsList("139856")).thenReturn(buildList);
        when(errataClient.getRelease("2096")).thenReturn(release);
        when(clientSession.getBuild(3338841)).thenReturn(kojiBuildInfo);
        when(requestEventRepository.updateRequestConfig(requestEvent, requestConfig))
                .thenReturn(initializedRequestEvent);

        ArgumentMatcher<RequestEvent> hasAdvisoryId = cfg -> cfg != null
                && "139856".equals(((ErrataAdvisoryRequestConfig) cfg.getRequestConfig()).getAdvisoryId());

        ApplicationException exception = new ApplicationException(
                "The standard errata advisory has unknown content-types (unknown).");
        doThrow(exception).when(advisoryService).generateFromAdvisory(argThat(hasAdvisoryId));

        // Verify that the handler throws the exception
        assertThrows(ApplicationException.class, () -> {
            // Call the handler method
            errataNotificationHandler.handle(requestEvent);
        });
    }

    @Test
    void testHandleErrataWithShippedLiveStatus() throws KojiClientException, IOException, JsonProcessingException {

        String errataJsonString = TestResources.asString("errata/api/erratum_SHIPPED_LIVE.json");
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

        // Create the initial requestEvent
        RequestEvent requestEvent = RequestEvent.createNew(null, RequestEventType.UMB, event).save();

        RequestConfig requestConfig = ErrataAdvisoryRequestConfig.builder().withAdvisoryId("139856").build();
        RequestEvent initializedRequestEvent = RequestEvent.createNew(requestConfig, RequestEventType.UMB, event);
        initializedRequestEvent.setId(requestEvent.getId());

        when(errataClient.getErratum("139856")).thenReturn(errata);
        when(errataClient.getBuildsList("139856")).thenReturn(buildList);
        when(errataClient.getRelease("2096")).thenReturn(release);
        when(clientSession.getBuild(3338841)).thenReturn(kojiBuildInfo);
        when(requestEventRepository.updateRequestConfig(requestEvent, requestConfig))
                .thenReturn(initializedRequestEvent);

        ArgumentMatcher<RequestEvent> hasAdvisoryId = cfg -> cfg != null
                && "139856".equals(((ErrataAdvisoryRequestConfig) cfg.getRequestConfig()).getAdvisoryId());

        ClientException exception = new ClientException(
                "Standard advisories with SHIPPED-LIVE are not handled yet. Stay tuned!");
        doThrow(exception).when(advisoryService).generateFromAdvisory(argThat(hasAdvisoryId));

        // Verify that the handler throws the exception
        assertThrows(ClientException.class, () -> {
            // Call the handler method
            errataNotificationHandler.handle(requestEvent);
        });
    }

    private SbomGenerationRequest expectPncBuildSbomGenerationRequest(String buildId) {
        return SbomGenerationRequest.builder()
                .withIdentifier(buildId)
                .withType(GenerationRequestType.BUILD)
                .withStatus(SbomGenerationStatus.NEW)
                .withConfig(PncBuildConfig.builder().withBuildId(buildId).build())
                .build();
    }

    private SbomGenerationRequest expectPncOperationSbomGenerationRequest(String operationId) {
        return SbomGenerationRequest.builder()
                .withIdentifier(operationId)
                .withType(GenerationRequestType.OPERATION)
                .withStatus(SbomGenerationStatus.NEW)
                .withConfig(OperationConfig.builder().withOperationId(operationId).build())
                .build();
    }

    private SbomGenerationRequest expectPncAnalysisSbomGenerationRequest(String operationId, List<String> urls) {

        return SbomGenerationRequest.builder()
                .withIdentifier(operationId)
                .withType(GenerationRequestType.OPERATION)
                .withStatus(SbomGenerationStatus.NO_OP)
                .withConfig(OperationConfig.builder().withDeliverableUrls(urls).withOperationId(operationId).build())
                .build();
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
