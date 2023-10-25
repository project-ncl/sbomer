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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.jboss.sbomer.core.features.sbom.config.runtime.DefaultProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.GeneratorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.ProcessorConfig;
import org.jboss.sbomer.core.features.sbom.config.runtime.RedHatProductProcessorConfig;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.rest.QueryParameters;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;
import org.jboss.sbomer.service.test.utils.QuarkusTransactionalTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.quarkus.logging.Log;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@QuarkusTransactionalTest
@WithKubernetesTestServer
public class SbomRepositoryIT {

    @Inject
    Validator validator;

    @Inject
    SbomRepository sbomRepository;

    @Test
    public void testNonNullRootComponents() {
        String rsqlQuery = "buildId=eq=ARYT3LBXDVYAC";
        Sbom sbom = sbomRepository.search(QueryParameters.builder().pageSize(1).rsqlQuery(rsqlQuery).build()).get(0);

        assertNotNull(sbom.getRootPurl());
        assertEquals(
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom",
                sbom.getRootPurl());
    }

    @Test
    public void testValidBom() throws JsonProcessingException, JsonMappingException {
        String rsqlQuery = "buildId=eq=ARYT3LBXDVYAC";
        Sbom sbom = sbomRepository.search(QueryParameters.builder().pageSize(1).rsqlQuery(rsqlQuery).build()).get(0);
        Bom bom = sbom.getCycloneDxBom();

        assertEquals("CycloneDX", bom.getBomFormat());
        Component firstComponent = bom.getComponents().get(0);
        assertEquals("microprofile-graphql-spec", firstComponent.getName());
        assertEquals(
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-spec@1.1.0.redhat-00008?type=pom",
                firstComponent.getPurl());
        assertEquals("416640206274228224", sbom.getId());
        assertEquals("ARYT3LBXDVYAC", sbom.getBuildId());

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
        Sbom sbom = sbomRepository.search(QueryParameters.builder().pageSize(10).rsqlQuery(rsqlQuery).build()).get(0);

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
        assertEquals(GeneratorType.MAVEN_DOMINO, generatorConfig.getType());
        assertEquals(
                "--config-file .domino/manifest/quarkus-bom-config.json --warn-on-missing-scm",
                generatorConfig.getArgs());
        assertEquals("0.0.90", generatorConfig.getVersion());

        assertEquals(2, processorConfigs.size());

        DefaultProcessorConfig defaultProcessorConfig = (DefaultProcessorConfig) processorConfigs.get(0);
        assertEquals(List.of("default"), defaultProcessorConfig.toCommand());

        RedHatProductProcessorConfig redHatProductProcessorConfig = (RedHatProductProcessorConfig) processorConfigs
                .get(1);
        assertEquals("RHBQ", redHatProductProcessorConfig.getErrata().getProductName());
        assertEquals("RHEL-8-RHBQ-2.13", redHatProductProcessorConfig.getErrata().getProductVersion());
        assertEquals("8Base-RHBQ-2.13", redHatProductProcessorConfig.getErrata().getProductVariant());
    }

    @Test
    public void testFindByIdSbom() {
        Sbom sbom = sbomRepository.findById("416640206274228224");

        assertEquals("416640206274228224", sbom.getId());
        assertEquals("ARYT3LBXDVYAC", sbom.getBuildId());
    }

}
