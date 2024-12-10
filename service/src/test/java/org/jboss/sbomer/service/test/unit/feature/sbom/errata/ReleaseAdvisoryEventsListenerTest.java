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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Evidence;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.component.evidence.Identity;
import org.cyclonedx.model.component.evidence.Identity.Field;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1RequestRecord;
import org.jboss.sbomer.core.features.sbom.enums.GenerationRequestType;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.BuildItem;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList.ProductVersionEntry;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataVariant;
import org.jboss.sbomer.service.feature.sbom.errata.event.release.AdvisoryReleaseEvent;
import org.jboss.sbomer.service.feature.sbom.errata.event.release.ReleaseAdvisoryEventsListener;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.RandomStringIdGenerator;
import org.jboss.sbomer.service.feature.sbom.model.RequestEvent;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.model.Stats;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.pyxis.PyxisClient;
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.PyxisRepositoryDetails;
import org.jboss.sbomer.service.feature.sbom.service.SbomGenerationRequestRepository;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.stats.StatsService;
import org.jboss.sbomer.service.test.utils.QuarkusTransactionalTest;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.SerializationFeature;

import groovy.util.logging.Slf4j;

@QuarkusTransactionalTest
@Slf4j
public class ReleaseAdvisoryEventsListenerTest {

    static class ReleaseAdvisoryEventsListenerSingleContainer extends ReleaseAdvisoryEventsListener {

        @Override
        protected Sbom saveReleaseManifestForPendingGeneration(SbomGenerationRequest pendingGeneration, Bom bom) {

            Metadata metadata = bom.getMetadata();
            Component metadataComponent = metadata.getComponent();
            Evidence evidence = metadataComponent.getEvidence();

            Component.Type type = metadataComponent.getType();
            String name = metadataComponent.getName();
            String version = metadataComponent.getVersion();
            String bomRef = metadataComponent.getBomRef();

            assertEquals(Component.Type.OPERATING_SYSTEM, type);
            assertEquals("Red Hat Enterprise Linux 8", name);
            assertEquals("RHEL-8.10.0.Z.MAIN+EUS", version);
            assertEquals("RHEL-8.10.0.Z.MAIN+EUS", bomRef);

            List<Identity> identities = evidence.getIdentities();
            List<String> expectedConcludedCPEValues = List.of(
                    "cpe:/a:redhat:enterprise_linux:8.10::appstream",
                    "cpe:/a:redhat:enterprise_linux:8::appstream");

            assertEquals(expectedConcludedCPEValues.size(), identities.size());

            for (int i = 0; i < expectedConcludedCPEValues.size(); i++) {
                assertEquals(expectedConcludedCPEValues.get(i), identities.get(i).getConcludedValue());
                assertEquals(Field.CPE, identities.get(i).getField());
            }

            Component mainComponent = bom.getComponents().get(0);
            Component.Type mType = mainComponent.getType();
            String mName = mainComponent.getName();
            String mVersion = mainComponent.getVersion();
            String mBomRef = mainComponent.getBomRef();
            String mPurl = mainComponent.getPurl();

            assertEquals(Component.Type.CONTAINER, mType);
            assertEquals("ubi8/ruby-25", mName);
            assertEquals("sha256:b1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94", mVersion);
            assertEquals(
                    "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94",
                    mBomRef);
            assertEquals(
                    "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94",
                    mPurl);
            assertEquals(4, mainComponent.getEvidence().getIdentities().size());

            List<String> expectedConcludedPurlValues = List.of(
                    "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94?repository_url=registry.access.redhat.com%2Fubi8%2Fruby-25&tag=1",
                    "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94?repository_url=registry.access.redhat.com%2Fubi8%2Fruby-25&tag=1-260.1733408998",
                    "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94?repository_url=registry.access.redhat.com%2Frhel8%2Fruby-25&tag=1-260.1733408998",
                    "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94?repository_url=registry.access.redhat.com%2Frhel8%2Fruby-25&tag=1");

            List<Identity> mIdentities = mainComponent.getEvidence().getIdentities();
            assertEquals(expectedConcludedPurlValues.size(), mIdentities.size());

            for (int i = 0; i < expectedConcludedPurlValues.size(); i++) {
                assertEquals(expectedConcludedPurlValues.get(i), mIdentities.get(i).getConcludedValue());
                assertEquals(Field.PURL, mIdentities.get(i).getField());
            }

            List<Dependency> dependencies = bom.getDependencies();
            assertEquals(1, dependencies.size());
            assertEquals("RHEL-8.10.0.Z.MAIN+EUS", dependencies.get(0).getRef());
            assertNull(dependencies.get(0).getDependencies());
            assertEquals(1, dependencies.get(0).getProvides().size());
            assertEquals(
                    "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94",
                    dependencies.get(0).getProvides().get(0).getRef());

            printRawBom(bom);
            return null;
        }
    }

    ReleaseAdvisoryEventsListenerSingleContainer listener;
    ErrataClient errataClient = mock(ErrataClient.class);
    PyxisClient pyxisClient = mock(PyxisClient.class);
    StatsService statsService = mock(StatsService.class);
    SbomService sbomService = mock(SbomService.class);
    SbomGenerationRequestRepository generationRequestRepository = mock(SbomGenerationRequestRepository.class);

    private static void printRawBom(Bom bom) {
        try {
            String jsonString = ObjectMapperProvider.json()
                    .enable(SerializationFeature.INDENT_OUTPUT)
                    .writeValueAsString(bom);
            System.out.println(jsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testReleaseErrataWithDockerBuilds() throws IOException {

        listener = new ReleaseAdvisoryEventsListenerSingleContainer();
        listener.setErrataClient(errataClient);
        listener.setPyxisClient(pyxisClient);
        listener.setStatsService(statsService);
        listener.setSbomService(sbomService);
        listener.setGenerationRequestRepository(generationRequestRepository);

        // Get all objects required
        String rawErrata = TestResources.asString("errata/release/singleContainer/errata_143793.json");
        String rawErrataBuildList = TestResources
                .asString("errata/release/singleContainer/errata_143793_build_list.json");
        String rawErrataVariant = TestResources.asString("errata/release/singleContainer/errata_143793_variant.json");
        String rawErrataRecords = TestResources.asString("errata/release/singleContainer/errata_143793_records.json");
        String rawEvent = TestResources.asString("errata/release/singleContainer/request_event.json");
        String rawFirstManifest = TestResources.asString("errata/release/singleContainer/A14FF4DDB7DB47D.json");
        String rawIndexManifest = TestResources.asString("errata/release/singleContainer/2A5F7CA4166C470.json");
        String rawPyxisRepositoryDetails = TestResources.asString("errata/release/singleContainer/pyxis.json");

        Errata errata = ObjectMapperProvider.json().readValue(rawErrata, Errata.class);
        ErrataBuildList erratumBuildList = ObjectMapperProvider.json()
                .readValue(rawErrataBuildList, ErrataBuildList.class);
        ErrataVariant variant = ObjectMapperProvider.json().readValue(rawErrataVariant, ErrataVariant.class);
        List<V1Beta1RequestRecord> allAdvisoryRequestRecords = ObjectMapperProvider.json()
                .readValue(rawErrataRecords, new TypeReference<List<V1Beta1RequestRecord>>() {
                });
        RequestEvent requestEvent = ObjectMapperProvider.json().readValue(rawEvent, RequestEvent.class);
        Sbom firstManifest = ObjectMapperProvider.json().readValue(rawFirstManifest, Sbom.class);
        Sbom indexManifest = ObjectMapperProvider.json().readValue(rawIndexManifest, Sbom.class);
        PyxisRepositoryDetails repositoriesDetails = ObjectMapperProvider.json()
                .readValue(rawPyxisRepositoryDetails, PyxisRepositoryDetails.class);

        Map<ProductVersionEntry, List<BuildItem>> buildDetails = erratumBuildList.getProductVersions()
                .values()
                .stream()
                .collect(
                        Collectors.toMap(
                                productVersionEntry -> productVersionEntry,
                                productVersionEntry -> productVersionEntry.getBuilds()
                                        .stream()
                                        .flatMap(build -> build.getBuildItems().values().stream())
                                        .collect(Collectors.toList())));

        V1Beta1RequestRecord latestAdvisoryRequestManifest = allAdvisoryRequestRecords.get(0);
        Map<ProductVersionEntry, SbomGenerationRequest> pvToGenerations = new HashMap<ProductVersionEntry, SbomGenerationRequest>();
        Map<String, SbomGenerationRequest> generationsMap = new HashMap<String, SbomGenerationRequest>();

        buildDetails.keySet().forEach(pv -> {
            String generationId = RandomStringIdGenerator.generate();
            SbomGenerationRequest sbomGenerationRequest = SbomGenerationRequest.builder()
                    .withId(generationId)
                    .withIdentifier(errata.getDetails().get().getFulladvisory() + "#" + pv.getName())
                    .withType(GenerationRequestType.CONTAINERIMAGE)
                    .withStatus(SbomGenerationStatus.GENERATING)
                    .withConfig(null) // I really don't know what to put here
                    .withRequest(requestEvent)
                    .build();

            generationsMap.put(generationId, sbomGenerationRequest);
            pvToGenerations.put(pv, sbomGenerationRequest);
        });
        when(errataClient.getVariant("AppStream-8.10.0.Z.MAIN.EUS")).thenReturn(variant);
        when(statsService.getStats())
                .thenReturn(Stats.builder().withVersion("ReleaseAdvisoryEventsListenerTest_1.0.0").build());
        when(sbomService.get("A14FF4DDB7DB47D")).thenReturn(firstManifest);
        when(sbomService.get("2A5F7CA4166C470")).thenReturn(indexManifest);
        when(pyxisClient.getRepositoriesDetails("ruby-25-container-1-260.1733408998")).thenReturn(repositoriesDetails);
        when(generationRequestRepository.findById(anyString())).thenAnswer(invocation -> {
            String generationId = invocation.getArgument(0);
            return generationsMap.get(generationId);
        });
        when(sbomService.save(any(Sbom.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdvisoryReleaseEvent event = AdvisoryReleaseEvent.builder()
                .withAdvisoryBuildDetails(buildDetails)
                .withErratum(errata)
                .withLatestAdvisoryManifestsRecord(latestAdvisoryRequestManifest)
                .withReleaseGenerations(pvToGenerations)
                .build();
        listener.onReleaseAdvisoryEvent(event);
    }

}
