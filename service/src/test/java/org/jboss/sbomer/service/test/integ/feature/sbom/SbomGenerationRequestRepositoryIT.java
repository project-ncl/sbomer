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
package org.jboss.sbomer.service.test.integ.feature.sbom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;

import jakarta.annotation.PostConstruct;
import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.config.runtime.DefaultProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ErrataConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.RedHatProductProcessorConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.rest.QueryParameters;
import org.jboss.sbomer.service.feature.sbom.service.SbomGenerationRequestRepository;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;
import org.jboss.sbomer.service.feature.sbom.service.SbomService;
import org.jboss.sbomer.service.test.utils.QuarkusTransactionalTest;
import org.junit.jupiter.api.Test;

import io.quarkus.panache.common.Parameters;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

@ApplicationScoped
@QuarkusTransactionalTest
@WithKubernetesTestServer
public class SbomGenerationRequestRepositoryIT {

    @Inject
    Validator validator;

    @Inject
    SbomRepository sbomRepository;

    @Inject
    SbomGenerationRequestRepository sbomGenerationRequestRepository;

    @Inject
    SbomService sbomService;

    final static String REQUEST_ID = "FFAASSBB";
    final static String BUILD_ID = "RRYT3LBXDVYAC";

    final static String REQUEST_ID_2_DELETE = "FFAASSBBDD";
    final static String BUILD_ID_2_DELETE = "RRYT3LBXDVYACDD";

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "sboms", fileName);
    }

    private Config createRuntimeConfig(String buildId) {
        GeneratorConfig generatorConfig = GeneratorConfig.builder()
                .type(GeneratorType.MAVEN_CYCLONEDX)
                .args("--include-non-managed --warn-on-missing-scm")
                .version("0.0.90")
                .build();

        DefaultProcessorConfig defaultProcessorConfig = DefaultProcessorConfig.builder().build();
        RedHatProductProcessorConfig redHatProductProcessorConfig = RedHatProductProcessorConfig.builder()
                .withErrata(
                        ErrataConfig.builder()
                                .productName("CCCDDD")
                                .productVersion("CCDD")
                                .productVariant("CD")
                                .build())
                .build();
        ProductConfig productConfig = ProductConfig.builder()
                .withGenerator(generatorConfig)
                .withProcessors(List.of(defaultProcessorConfig, redHatProductProcessorConfig))
                .build();

        return Config.builder()
                .withApiVersion("sbomer.jboss.org/v1alpha1")
                .withBuildId(buildId)
                .withProducts(List.of(productConfig))
                .withEnvironment(Map.of("k", "v"))
                .build();
    }

    private Sbom createSBOM() throws IOException {
        Bom bom = SbomUtils.fromPath(sbomPath("complete_sbom.json"));
        Config runtimeConfig = createRuntimeConfig(BUILD_ID);

        SbomGenerationRequest generationRequest = SbomGenerationRequest.builder()
                .withConfig(SbomUtils.toJsonNode(runtimeConfig))
                .withId(REQUEST_ID)
                .withBuildId(BUILD_ID)
                .withStatus(SbomGenerationStatus.FINISHED)
                .build();

        // Not setting rootPurl, as it will be set by PrePersist
        Sbom sbom = new Sbom();
        sbom.setBuildId(BUILD_ID);
        sbom.setId("416640206274228333");
        sbom.setSbom(SbomUtils.toJsonNode(bom));
        sbom.setGenerationRequest(generationRequest);
        return sbom;
    }

    @PostConstruct
    public void init() throws Exception {
        Sbom sbom = createSBOM();
        sbomRepository.saveSbom(sbom);
    }

    @Test
    public void testRSQL() {
        SbomGenerationRequest request = sbomGenerationRequestRepository
                .search(QueryParameters.builder().rsqlQuery("buildId=eq=" + BUILD_ID).pageIndex(0).pageSize(1).build())
                .get(0);

        assertNotNull(request);
        assertNotNull(request.getCreationTime());
        assertEquals(REQUEST_ID, request.getId());
        assertEquals(BUILD_ID, request.getBuildId());

        request = sbomGenerationRequestRepository
                .search(QueryParameters.builder().rsqlQuery("buildId=eq=" + BUILD_ID).pageIndex(0).pageSize(1).build())
                .get(0);

        assertNotNull(request);
        assertEquals(REQUEST_ID, request.getId());
        assertEquals(BUILD_ID, request.getBuildId());
        assertEquals("FINISHED".toLowerCase(), request.getStatus().toName());
    }

    @Test
    public void testPagination() {
        Page<SbomGenerationRequest> pagedRequest = sbomService
                .searchSbomRequestsByQueryPaginated(0, 10, "buildId=eq=" + BUILD_ID, null);

        assertNotNull(pagedRequest);
        assertEquals(0, pagedRequest.getPageIndex());
        assertEquals(10, pagedRequest.getPageSize());
        assertEquals(1, pagedRequest.getTotalHits());
        assertEquals(1, pagedRequest.getTotalPages());
        assertEquals(1, pagedRequest.getContent().size());
        SbomGenerationRequest request = pagedRequest.getContent().iterator().next();
        assertNotNull(request.getCreationTime());
        assertEquals(REQUEST_ID, request.getId());
        assertEquals(BUILD_ID, request.getBuildId());
    }

    @Test
    public void testFindByIdSbomGenerationRequest() {
        SbomGenerationRequest request = SbomGenerationRequest.findById(REQUEST_ID);

        assertEquals(REQUEST_ID, request.getId());
        assertEquals(BUILD_ID, request.getBuildId());
    }

    @Test
    public void testDeleteSbomGenerationRequest() throws Exception {
        Config runtimeConfig = createRuntimeConfig(BUILD_ID_2_DELETE);

        SbomGenerationRequest generationRequest = SbomGenerationRequest.builder()
                .withConfig(SbomUtils.toJsonNode(runtimeConfig))
                .withId(REQUEST_ID_2_DELETE)
                .withBuildId(BUILD_ID_2_DELETE)
                .withStatus(SbomGenerationStatus.FINISHED)
                .build();

        Sbom sbom = createSBOM();
        sbom.setId("13");
        sbom.setBuildId(BUILD_ID_2_DELETE);
        sbom.setGenerationRequest(generationRequest);
        sbom = sbomRepository.saveSbom(sbom);

        String rsqlQuery = "generationRequest.id=eq=" + REQUEST_ID_2_DELETE;
        List<Sbom> sbomsAfterInsert = sbomRepository
                .search(QueryParameters.builder().pageSize(10).rsqlQuery(rsqlQuery).build());
        assertEquals(1, sbomsAfterInsert.size());

        long beforeDeletion = SbomGenerationRequest.count("id = :id", Parameters.with("id", REQUEST_ID_2_DELETE));
        assertEquals(1, beforeDeletion);

        sbomGenerationRequestRepository.deleteRequest(REQUEST_ID_2_DELETE);

        List<Sbom> sbomsAfterDelete = sbomRepository
                .search(QueryParameters.builder().pageSize(10).rsqlQuery(rsqlQuery).build());
        assertEquals(0, sbomsAfterDelete.size());

        long afterDeletion = SbomGenerationRequest.count("id = :id", Parameters.with("id", REQUEST_ID_2_DELETE));
        assertEquals(0, afterDeletion);

    }

}
