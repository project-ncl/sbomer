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
import static org.junit.Assert.assertTrue;
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
import org.jboss.sbomer.service.feature.sbom.pyxis.dto.RepositoryCoordinates;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;
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

    ReleaseAdvisoryEventsListenerSingleContainer listenerSingle;
    ReleaseAdvisoryEventsListenerMultiContainer listenerMulti;
    ErrataClient errataClient = mock(ErrataClient.class);
    PyxisClient pyxisClient = mock(PyxisClient.class);
    StatsService statsService = mock(StatsService.class);
    SbomService sbomService = mock(SbomService.class);
    SbomGenerationRequestRepository generationRequestRepository = mock(SbomGenerationRequestRepository.class);
    RequestEventRepository requestEventRepository = mock(RequestEventRepository.class);

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

    private static void validateComponent(
            Component component,
            Component.Type expectedType,
            String expectedName,
            String expectedVersion,
            String expectedBomRef,
            String expectedPurl,
            List<String> expectedConcludedValues,
            Field expectedField) {
        assertEquals(expectedType, component.getType());
        assertEquals(expectedName, component.getName());
        assertEquals(expectedVersion, component.getVersion());
        assertEquals(expectedBomRef, component.getBomRef());
        assertEquals(expectedPurl, component.getPurl());

        List<Identity> identities = component.getEvidence().getIdentities();
        assertEquals(expectedConcludedValues.size(), identities.size());
        for (int i = 0; i < expectedConcludedValues.size(); i++) {
            assertEquals(expectedConcludedValues.get(i), identities.get(i).getConcludedValue());
            assertEquals(expectedField, identities.get(i).getField());
        }
    }

    private static void validateDependencies(
            List<Dependency> dependencies,
            int expectedDepSize,
            String expectedMainDepRef,
            List<String> expectedProvidesRef) {
        assertEquals(expectedDepSize, dependencies.size());
        assertEquals(expectedMainDepRef, dependencies.get(0).getRef());
        assertNull(dependencies.get(0).getDependencies());
        assertEquals(expectedProvidesRef.size(), dependencies.get(0).getProvides().size());
        for (int i = 0; i < expectedProvidesRef.size(); i++) {
            assertEquals(expectedProvidesRef.get(i), dependencies.get(0).getProvides().get(i).getRef());
        }
    }

    static class ReleaseAdvisoryEventsListenerSingleContainer extends ReleaseAdvisoryEventsListener {

        @Override
        protected Sbom saveReleaseManifestForDockerGeneration(
                SbomGenerationRequest releaseGeneration,
                Bom bom,
                V1Beta1RequestRecord advisoryManifestsRecord,
                Map<String, List<RepositoryCoordinates>> generationToRepositories) {

            validateComponent(
                    bom.getMetadata().getComponent(),
                    Component.Type.OPERATING_SYSTEM,
                    "Red Hat Enterprise Linux 8",
                    "RHEL-8.10.0.Z.MAIN+EUS",
                    "RHEL-8.10.0.Z.MAIN+EUS",
                    null,
                    List.of(
                            "cpe:/a:redhat:enterprise_linux:8.10::appstream",
                            "cpe:/a:redhat:enterprise_linux:8::appstream"),
                    Field.CPE);

            validateComponent(
                    bom.getComponents().get(0),
                    Component.Type.CONTAINER,
                    "ubi8/ruby-25",
                    "sha256:b1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94",
                    "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94",
                    "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94",
                    List.of(
                            "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94?repository_url=registry.access.redhat.com%2Fubi8%2Fruby-25&tag=1",
                            "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94?repository_url=registry.access.redhat.com%2Fubi8%2Fruby-25&tag=1-260.1733408998",
                            "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94?repository_url=registry.access.redhat.com%2Frhel8%2Fruby-25&tag=1-260.1733408998",
                            "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94?repository_url=registry.access.redhat.com%2Frhel8%2Fruby-25&tag=1"),
                    Field.PURL);

            validateDependencies(
                    bom.getDependencies(),
                    1,
                    "RHEL-8.10.0.Z.MAIN+EUS",
                    List.of(
                            "pkg:oci/ruby-25@sha256%3Ab1f140e930baffe400e412fbf04d57624a18593e77fcc5cfa1b2462a3f85fc94"));

            printRawBom(bom);
            return null;
        }
    }

    @Test
    void testReleaseErrataWithSingleDockerBuild() throws IOException {

        listenerSingle = new ReleaseAdvisoryEventsListenerSingleContainer();
        listenerSingle.setErrataClient(errataClient);
        listenerSingle.setPyxisClient(pyxisClient);
        listenerSingle.setStatsService(statsService);
        listenerSingle.setSbomService(sbomService);
        listenerSingle.setGenerationRequestRepository(generationRequestRepository);
        listenerSingle.setRequestEventRepository(requestEventRepository);

        // Get all objects required
        Errata errata = loadErrata("singleContainer/errata_143793.json");
        ErrataBuildList erratumBuildList = loadErrataBuildList("singleContainer/errata_143793_build_list.json");
        ErrataVariant variant = loadErrataVariant("singleContainer/errata_143793_variant.json");
        List<V1Beta1RequestRecord> allAdvisoryRequestRecords = loadRequestRecords(
                "singleContainer/errata_143793_records.json");
        RequestEvent requestEvent = loadRequestEvent("singleContainer/request_event.json");
        Sbom firstManifest = loadSbom("singleContainer/A14FF4DDB7DB47D.json");
        Sbom indexManifest = loadSbom("singleContainer/2A5F7CA4166C470.json");
        PyxisRepositoryDetails repositoriesDetails = loadPyxisRepositoryDetails("singleContainer/pyxis.json");

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
        when(errataClient.getBuildsList(String.valueOf(errata.getDetails().get().getId())))
                .thenReturn(erratumBuildList);
        when(errataClient.getErratum(String.valueOf(errata.getDetails().get().getId()))).thenReturn(errata);

        when(statsService.getStats())
                .thenReturn(Stats.builder().withVersion("ReleaseAdvisoryEventsListenerTest_1.0.0").build());

        when(pyxisClient.getRepositoriesDetails("ruby-25-container-1-260.1733408998")).thenReturn(repositoriesDetails);

        when(generationRequestRepository.findById(anyString())).thenAnswer(invocation -> {
            String generationId = invocation.getArgument(0);
            return generationsMap.get(generationId);
        });
        when(requestEventRepository.findById(anyString())).thenReturn(requestEvent);

        when(sbomService.get("A14FF4DDB7DB47D")).thenReturn(firstManifest);
        when(sbomService.get("2A5F7CA4166C470")).thenReturn(indexManifest);
        when(sbomService.save(any(Sbom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sbomService.searchLastSuccessfulAdvisoryRequestRecord(anyString()))
                .thenReturn(latestAdvisoryRequestManifest);

        AdvisoryReleaseEvent event = AdvisoryReleaseEvent.builder()
                .withRequestEventId(requestEvent.getId())
                .withReleaseGenerations(pvToGenerations)
                .build();
        listenerSingle.onReleaseAdvisoryEvent(event);
    }

    static class ReleaseAdvisoryEventsListenerMultiContainer extends ReleaseAdvisoryEventsListener {

        @Override
        protected Sbom saveReleaseManifestForDockerGeneration(
                SbomGenerationRequest releaseGeneration,
                Bom bom,
                V1Beta1RequestRecord advisoryManifestsRecord,
                Map<String, List<RepositoryCoordinates>> generationToRepositories) {

            assertTrue(
                    bom.getMetadata().getComponent().getVersion().equals("OSE-4.15-RHEL-8")
                            || bom.getMetadata().getComponent().getVersion().equals("OSE-4.15-RHEL-9"));

            if (bom.getMetadata().getComponent().getVersion().equals("OSE-4.15-RHEL-8")) {

                validateComponent(
                        bom.getMetadata().getComponent(),
                        Component.Type.FRAMEWORK,
                        "Red Hat OpenShift Container Platform 4.15",
                        "OSE-4.15-RHEL-8",
                        "OSE-4.15-RHEL-8",
                        null,
                        List.of("cpe:/a:redhat:openshift:4.15::el8"),
                        Field.CPE);

                validateComponent(
                        bom.getComponents().get(0),
                        Component.Type.CONTAINER,
                        "openshift/ose-gcp-filestore-csi-driver-operator-bundle",
                        "sha256:201088e8a5c8a59bac4f8bc796542fb76b162e68e5af521f8dd56d05446f52f4",
                        "pkg:oci/ose-gcp-filestore-csi-driver-operator-bundle@sha256%3A201088e8a5c8a59bac4f8bc796542fb76b162e68e5af521f8dd56d05446f52f4",
                        "pkg:oci/ose-gcp-filestore-csi-driver-operator-bundle@sha256%3A201088e8a5c8a59bac4f8bc796542fb76b162e68e5af521f8dd56d05446f52f4",
                        List.of(
                                "pkg:oci/ose-gcp-filestore-csi-driver-operator-bundle@sha256%3A201088e8a5c8a59bac4f8bc796542fb76b162e68e5af521f8dd56d05446f52f4?repository_url=registry.access.redhat.com%2Fopenshift4%2Fose-gcp-filestore-csi-driver-operator-bundle&tag=v4.15.0.202412041605.p0.ga923e95.assembly.stream.el8",
                                "pkg:oci/ose-gcp-filestore-csi-driver-operator-bundle@sha256%3A201088e8a5c8a59bac4f8bc796542fb76b162e68e5af521f8dd56d05446f52f4?repository_url=registry.access.redhat.com%2Fopenshift4%2Fose-gcp-filestore-csi-driver-operator-bundle&tag=v4.15.0.202412041605.p0.ga923e95.assembly.stream.el8-2",
                                "pkg:oci/ose-gcp-filestore-csi-driver-operator-bundle@sha256%3A201088e8a5c8a59bac4f8bc796542fb76b162e68e5af521f8dd56d05446f52f4?repository_url=registry.access.redhat.com%2Fopenshift4%2Fose-gcp-filestore-csi-driver-operator-bundle&tag=v4.15"),
                        Field.PURL);

                validateDependencies(
                        bom.getDependencies(),
                        1,
                        "OSE-4.15-RHEL-8",
                        List.of(
                                "pkg:oci/ose-gcp-filestore-csi-driver-operator-bundle@sha256%3A201088e8a5c8a59bac4f8bc796542fb76b162e68e5af521f8dd56d05446f52f4",
                                "pkg:oci/ose-aws-efs-csi-driver-operator-bundle@sha256%3A6e697d697a394f08cb9a669c7a85cb867e02fbfba79848390a518771ad3558cf",
                                "pkg:oci/ose-secrets-store-csi-driver-operator-bundle@sha256%3Ac3eb47f4c962949a1f8446de39963f20b299fbd1bf27e742232f1a9c144e7be0"));

            } else if (bom.getMetadata().getComponent().getVersion().equals("OSE-4.15-RHEL-9")) {

                validateComponent(
                        bom.getMetadata().getComponent(),
                        Component.Type.FRAMEWORK,
                        "Red Hat OpenShift Container Platform 4.15",
                        "OSE-4.15-RHEL-9",
                        "OSE-4.15-RHEL-9",
                        null,
                        List.of("cpe:/a:redhat:openshift:4.15::el9"),
                        Field.CPE);

                validateComponent(
                        bom.getComponents().get(0),
                        Component.Type.CONTAINER,
                        "openshift/ose-clusterresourceoverride-operator-bundle",
                        "sha256:d37ea60be41f378e0d3b9c0936d8c3fb0e218e00b8cdc3c073a3e35d494f3e8d",
                        "pkg:oci/ose-clusterresourceoverride-operator-bundle@sha256%3Ad37ea60be41f378e0d3b9c0936d8c3fb0e218e00b8cdc3c073a3e35d494f3e8d",
                        "pkg:oci/ose-clusterresourceoverride-operator-bundle@sha256%3Ad37ea60be41f378e0d3b9c0936d8c3fb0e218e00b8cdc3c073a3e35d494f3e8d",
                        List.of(
                                "pkg:oci/ose-clusterresourceoverride-operator-bundle@sha256%3Ad37ea60be41f378e0d3b9c0936d8c3fb0e218e00b8cdc3c073a3e35d494f3e8d?repository_url=registry.access.redhat.com%2Fopenshift4%2Fose-clusterresourceoverride-operator-bundle&tag=v4.15.0.202412021736.p0.g40c168c.assembly.stream.el9-1",
                                "pkg:oci/ose-clusterresourceoverride-operator-bundle@sha256%3Ad37ea60be41f378e0d3b9c0936d8c3fb0e218e00b8cdc3c073a3e35d494f3e8d?repository_url=registry.access.redhat.com%2Fopenshift4%2Fose-clusterresourceoverride-operator-bundle&tag=v4.15",
                                "pkg:oci/ose-clusterresourceoverride-operator-bundle@sha256%3Ad37ea60be41f378e0d3b9c0936d8c3fb0e218e00b8cdc3c073a3e35d494f3e8d?repository_url=registry.access.redhat.com%2Fopenshift4%2Fose-clusterresourceoverride-operator-bundle&tag=v4.15.0.202412021736.p0.g40c168c.assembly.stream.el9"),
                        Field.PURL);

                validateDependencies(
                        bom.getDependencies(),
                        1,
                        "OSE-4.15-RHEL-9",
                        List.of(
                                "pkg:oci/ose-clusterresourceoverride-operator-bundle@sha256%3Ad37ea60be41f378e0d3b9c0936d8c3fb0e218e00b8cdc3c073a3e35d494f3e8d",
                                "pkg:oci/ose-vertical-pod-autoscaler-operator-bundle@sha256%3A0c1507509cd03b183011726b11dc0f7834af8a097c9ffd5a4b389b8f2eae3bad",
                                "pkg:oci/openshift-ose-cluster-nfd-operator-bundle@sha256%3A3b8a3f00c2eb483d25ad43a44e0140f699cd4d7a9a5c4f43e2eecc21ea8a6771",
                                "pkg:oci/openshift-ose-ingress-node-firewall-operator-bundle@sha256%3A13f5bc757b25b359680938a06bbc83609b87f6797ede99f97953e5464ed380ef",
                                "pkg:oci/openshift-ose-local-storage-operator-bundle@sha256%3A20f5923ea4ba9fdef4779efb1423274b479efbb1278dd13010518a159cf32e37",
                                "pkg:oci/openshift-ose-metallb-operator-bundle@sha256%3A28b9c5ae08d95f9ae01bbf9fabab2d0bb7c93104243652e2b934dace47e3426d",
                                "pkg:oci/openshift-ose-openshift-kubernetes-nmstate-operator-bundle@sha256%3A20c3af0b0c80da26b4ea948b705e073d9df7181e58852a89ec6ee783933f4275",
                                "pkg:oci/openshift-ose-ptp-operator-bundle@sha256%3Af0bcb875bc379e1c6a2508796d69febe3891c5717956bcade2e1df6708f376b9"));
            }

            printRawBom(bom);
            return null;
        }
    }

    @Test
    void testReleaseErrataWithMultiDockerBuilds() throws IOException {

        listenerMulti = new ReleaseAdvisoryEventsListenerMultiContainer();
        listenerMulti.setErrataClient(errataClient);
        listenerMulti.setPyxisClient(pyxisClient);
        listenerMulti.setStatsService(statsService);
        listenerMulti.setSbomService(sbomService);
        listenerMulti.setGenerationRequestRepository(generationRequestRepository);
        listenerMulti.setRequestEventRepository(requestEventRepository);

        // Get all objects required
        //
        Map<String, PyxisRepositoryDetails> pyxisRepositories = new HashMap<String, PyxisRepositoryDetails>();
        pyxisRepositories.put(
                "ose-clusterresourceoverride-operator-metadata-container-v4.15.0.202412021736.p0.g40c168c.assembly.stream.el9-1",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3421076.json"));
        pyxisRepositories.put(
                "ose-vertical-pod-autoscaler-operator-metadata-container-v4.15.0.202412021736.p0.g8876256.assembly.stream.el9-1",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3421077.json"));
        pyxisRepositories.put(
                "cluster-nfd-operator-metadata-container-v4.15.0.202412041605.p0.gabdfb61.assembly.stream.el9-2",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3427588.json"));
        pyxisRepositories.put(
                "ingress-node-firewall-operator-bundle-container-v4.15.0.202412041605.p0.g135f832.assembly.stream.el9-2",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3427589.json"));
        pyxisRepositories.put(
                "local-storage-operator-metadata-container-v4.15.0.202412041605.p0.gcc4f213.assembly.stream.el9-2",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3427592.json"));
        pyxisRepositories.put(
                "ose-metallb-operator-bundle-container-v4.15.0.202412041605.p0.g359620b.assembly.stream.el9-2",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3427593.json"));
        pyxisRepositories.put(
                "ose-kubernetes-nmstate-operator-bundle-container-v4.15.0.202412041605.p0.g0d290b4.assembly.stream.el9-2",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3427594.json"));
        pyxisRepositories.put(
                "ose-ptp-operator-metadata-container-v4.15.0.202412041605.p0.g6fb51fd.assembly.stream.el9-2",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3427595.json"));
        pyxisRepositories.put(
                "ose-gcp-filestore-csi-driver-operator-bundle-container-v4.15.0.202412041605.p0.ga923e95.assembly.stream.el8-2",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3427596.json"));
        pyxisRepositories.put(
                "ose-aws-efs-csi-driver-operator-bundle-container-v4.15.0.202412041605.p0.gb0f13a0.assembly.stream.el8-2",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3427616.json"));
        pyxisRepositories.put(
                "ose-secrets-store-csi-driver-operator-bundle-container-v4.15.0.202412041605.p0.gef602a5.assembly.stream.el8-2",
                loadPyxisRepositoryDetails("multiContainers/pyxis_3427615.json"));

        Errata errata = loadErrata("multiContainers/errata_143781.json");
        ErrataBuildList erratumBuildList = loadErrataBuildList("multiContainers/errata_143781_build_list.json");
        List<V1Beta1RequestRecord> allAdvisoryRequestRecords = loadRequestRecords(
                "multiContainers/errata_143781_records.json");
        RequestEvent requestEvent = loadRequestEvent("multiContainers/request_event.json");

        Map<String, Sbom> sboms = new HashMap<String, Sbom>();
        sboms.put("505A5B90871046D", loadSbom("multiContainers/505A5B90871046D.json"));
        sboms.put("415DE22239C9439", loadSbom("multiContainers/415DE22239C9439.json"));
        sboms.put("47F4BC6A1C1641B", loadSbom("multiContainers/47F4BC6A1C1641B.json"));
        sboms.put("972A7D1C755548A", loadSbom("multiContainers/972A7D1C755548A.json"));
        sboms.put("8FEBABB08A614E6", loadSbom("multiContainers/8FEBABB08A614E6.json"));
        sboms.put("D8820BD0227F430", loadSbom("multiContainers/D8820BD0227F430.json"));
        sboms.put("2B7738D0D6964CB", loadSbom("multiContainers/2B7738D0D6964CB.json"));
        sboms.put("96D539F40D73483", loadSbom("multiContainers/96D539F40D73483.json"));
        sboms.put("1251FF8155D74B5", loadSbom("multiContainers/1251FF8155D74B5.json"));
        sboms.put("CDCBA3AA014747E", loadSbom("multiContainers/CDCBA3AA014747E.json"));
        sboms.put("AB81F89CF06645A", loadSbom("multiContainers/AB81F89CF06645A.json"));
        sboms.put("6AC3057851CB480", loadSbom("multiContainers/6AC3057851CB480.json"));
        sboms.put("9598ACF8368F49E", loadSbom("multiContainers/9598ACF8368F49E.json"));
        sboms.put("C67D863805F6463", loadSbom("multiContainers/C67D863805F6463.json"));
        sboms.put("EAABD415A3C7420", loadSbom("multiContainers/EAABD415A3C7420.json"));
        sboms.put("1DEFC26D4700433", loadSbom("multiContainers/1DEFC26D4700433.json"));
        sboms.put("FD64B2D3C6484CF", loadSbom("multiContainers/FD64B2D3C6484CF.json"));
        sboms.put("E0527227ABA94B9", loadSbom("multiContainers/E0527227ABA94B9.json"));
        sboms.put("0B7A5E98F9CE46E", loadSbom("multiContainers/0B7A5E98F9CE46E.json"));
        sboms.put("5095B5376819410", loadSbom("multiContainers/5095B5376819410.json"));
        sboms.put("3192918668C2431", loadSbom("multiContainers/3192918668C2431.json"));
        sboms.put("DC8F6347901E470", loadSbom("multiContainers/DC8F6347901E470.json"));

        ErrataVariant variant8BaseRHOSE415 = loadErrataVariant(
                "multiContainers/errata_143781_variant_8Base-RHOSE-4.15.json");
        ErrataVariant variant9BaseRHOSE415 = loadErrataVariant(
                "multiContainers/errata_143781_variant_9Base-RHOSE-4.15.json");

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

        when(errataClient.getVariant("8Base-RHOSE-4.15")).thenReturn(variant8BaseRHOSE415);
        when(errataClient.getVariant("9Base-RHOSE-4.15")).thenReturn(variant9BaseRHOSE415);
        when(errataClient.getBuildsList(String.valueOf(errata.getDetails().get().getId())))
                .thenReturn(erratumBuildList);
        when(errataClient.getErratum(String.valueOf(errata.getDetails().get().getId()))).thenReturn(errata);

        when(statsService.getStats())
                .thenReturn(Stats.builder().withVersion("ReleaseAdvisoryEventsListenerTest_1.0.0").build());

        when(pyxisClient.getRepositoriesDetails(anyString()))
                .thenAnswer(invocation -> pyxisRepositories.get(invocation.getArgument(0)));

        when(generationRequestRepository.findById(anyString())).thenAnswer(invocation -> {
            String generationId = invocation.getArgument(0);
            return generationsMap.get(generationId);
        });
        when(requestEventRepository.findById(anyString())).thenReturn(requestEvent);

        when(sbomService.get(anyString())).thenAnswer(invocation -> sboms.get(invocation.getArgument(0)));
        when(sbomService.save(any(Sbom.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sbomService.searchLastSuccessfulAdvisoryRequestRecord(anyString()))
                .thenReturn(latestAdvisoryRequestManifest);

        AdvisoryReleaseEvent event = AdvisoryReleaseEvent.builder()
                .withRequestEventId(requestEvent.getId())
                .withReleaseGenerations(pvToGenerations)
                .build();
        listenerMulti.onReleaseAdvisoryEvent(event);
    }

    private PyxisRepositoryDetails loadPyxisRepositoryDetails(String fileName) throws IOException {
        return parseResource(fileName, PyxisRepositoryDetails.class);
    }

    private Sbom loadSbom(String fileName) throws IOException {
        return parseResource(fileName, Sbom.class);
    }

    private Errata loadErrata(String fileName) throws IOException {
        return parseResource(fileName, Errata.class);
    }

    private ErrataVariant loadErrataVariant(String fileName) throws IOException {
        return parseResource(fileName, ErrataVariant.class);
    }

    private ErrataBuildList loadErrataBuildList(String fileName) throws IOException {
        return parseResource(fileName, ErrataBuildList.class);
    }

    private RequestEvent loadRequestEvent(String fileName) throws IOException {
        return parseResource(fileName, RequestEvent.class);
    }

    private List<V1Beta1RequestRecord> loadRequestRecords(String fileName) throws IOException {
        return parseResource(fileName, new TypeReference<List<V1Beta1RequestRecord>>() {
        });
    }

    private <T> T parseResource(String fileName, Class<T> type) throws IOException {
        return ObjectMapperProvider.json().readValue(TestResources.asString("errata/release/" + fileName), type);
    }

    private <T> T parseResource(String fileName, TypeReference<T> typeReference) throws IOException {
        return ObjectMapperProvider.json()
                .readValue(TestResources.asString("errata/release/" + fileName), typeReference);
    }

}
