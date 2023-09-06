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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.annotation.PostConstruct;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.jboss.sbomer.core.features.sbom.config.runtime.Config;
import org.jboss.sbomer.core.features.sbom.config.runtime.DefaultProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ErrataConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProductConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.RedHatProductProcessorConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.k8s.model.SbomGenerationStatus;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;
import org.jboss.sbomer.service.test.utils.QuarkusTransactionalTest;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.quarkus.logging.Log;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

@ApplicationScoped
@QuarkusTransactionalTest
@WithKubernetesTestServer
public class SbomRepositoryIT {

    @Inject
    Validator validator;

    @Inject
    SbomRepository sbomRepository;

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
                .errata(
                        ErrataConfig.builder()
                                .productName("CCCDDD")
                                .productVersion("CCDD")
                                .productVariant("CD")
                                .build())
                .build();
        ProductConfig productConfig = ProductConfig.builder()
                .generator(generatorConfig)
                .processors(List.of(defaultProcessorConfig, redHatProductProcessorConfig))
                .build();

        return Config.builder()
                .apiVersion("sbomer.jboss.org/v1alpha1")
                .buildId(buildId)
                .products(List.of(productConfig))
                .build();
    }

    private Sbom createSBOM() throws IOException {
        String buildId = "ARYT3LBXDVYAC";
        Bom bom = SbomUtils.fromPath(sbomPath("complete_sbom.json"));
        Config runtimeConfig = createRuntimeConfig(buildId);

        SbomGenerationRequest generationRequest = SbomGenerationRequest.builder()
                .withConfig(SbomUtils.toJsonNode(runtimeConfig))
                .withId("AASSBB")
                .withBuildId(buildId)
                .withStatus(SbomGenerationStatus.FINISHED)
                .build();

        // Not setting rootPurl, as it will be set by PrePersist
        Sbom sbom = new Sbom();
        sbom.setBuildId(buildId);
        sbom.setId("416640206274228224");
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
    public void testNonNullRootComponents() {
        String rsqlQuery = "buildId=eq=ARYT3LBXDVYAC";
        Sbom sbom = sbomRepository.searchByQuery(0, 1, rsqlQuery, null).get(0);

        assertNotNull(sbom.getRootPurl());
        assertEquals(
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom",
                sbom.getRootPurl());
    }

    @Test
    public void testValidBom() throws JsonProcessingException, JsonMappingException {
        String rsqlQuery = "buildId=eq=ARYT3LBXDVYAC";
        Sbom sbom = sbomRepository.searchByQuery(0, 1, rsqlQuery, null).get(0);
        Bom bom = sbom.getCycloneDxBom();

        assertEquals("416640206274228224", sbom.getId());
        assertEquals("ARYT3LBXDVYAC", sbom.getBuildId());
        assertEquals("CycloneDX", bom.getBomFormat());
        Component firstComponent = bom.getComponents().get(0);
        assertEquals("microprofile-graphql-spec", firstComponent.getName());
        assertEquals(
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-spec@1.1.0.redhat-00008?type=pom",
                firstComponent.getPurl());

        Set<ConstraintViolation<Sbom>> violations = validator.validate(sbom);
        if (!violations.isEmpty()) {
            Log.error(
                    "violations: " + violations.stream()
                            .map(e -> e.getMessage().toString())
                            .collect(Collectors.joining("\n\t")));
            fail("Validation errors on the baseSBOM entity should be empty!");
        }
    }

    @Test
    public void testValidConfiguration() throws JsonProcessingException, JsonMappingException {
        String rsqlQuery = "buildId=eq=ARYT3LBXDVYAC";
        Sbom sbom = sbomRepository.searchByQuery(0, 1, rsqlQuery, null).get(0);

        assertEquals("416640206274228224", sbom.getId());
        assertEquals("ARYT3LBXDVYAC", sbom.getGenerationRequest().getConfiguration().getBuildId());

        GeneratorConfig generatorConfig = sbom.getGenerationRequest()
                .getConfiguration()
                .getProducts()
                .iterator()
                .next()
                .getGenerator();
        List<ProcessorConfig> processorConfigs = sbom.getGenerationRequest()
                .getConfiguration()
                .getProducts()
                .iterator()
                .next()
                .getProcessors();
        assertEquals(GeneratorType.MAVEN_CYCLONEDX, generatorConfig.getType());
        assertEquals("--include-non-managed --warn-on-missing-scm", generatorConfig.getArgs());
        assertEquals("0.0.90", generatorConfig.getVersion());

        assertEquals(2, processorConfigs.size());

        DefaultProcessorConfig defaultProcessorConfig = (DefaultProcessorConfig) processorConfigs.get(0);
        assertEquals(List.of("default"), defaultProcessorConfig.toCommand());

        RedHatProductProcessorConfig redHatProductProcessorConfig = (RedHatProductProcessorConfig) processorConfigs
                .get(1);
        assertEquals("CCCDDD", redHatProductProcessorConfig.getErrata().getProductName());
        assertEquals("CCDD", redHatProductProcessorConfig.getErrata().getProductVersion());
        assertEquals("CD", redHatProductProcessorConfig.getErrata().getProductVariant());
    }

    @Test
    public void testFindByIdSbom() {
        Sbom sbom = sbomRepository.findById("416640206274228224");

        assertEquals("416640206274228224", sbom.getId());
        assertEquals("ARYT3LBXDVYAC", sbom.getBuildId());
    }

}
