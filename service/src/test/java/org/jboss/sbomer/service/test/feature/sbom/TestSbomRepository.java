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
package org.jboss.sbomer.service.test.feature.sbom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;
import org.jboss.sbomer.core.features.sbom.enums.SbomStatus;
import org.jboss.sbomer.core.features.sbom.enums.SbomType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import io.quarkus.logging.Log;
import io.quarkus.test.kubernetes.client.WithKubernetesTestServer;

@ApplicationScoped
@QuarkusTransactionalTest
@WithKubernetesTestServer
public class TestSbomRepository {

    @Inject
    Validator validator;

    @Inject
    SbomRepository sbomRepository;

    static Path sbomPath(String fileName) {
        return Paths.get("src", "test", "resources", "sboms", fileName);
    }

    private Sbom createParentSBOM() throws IOException {
        Bom bom = SbomUtils.fromPath(sbomPath("base.json"));

        // Not setting rootPurl, as it will be set by PrePersist
        Sbom parentSBOM = new Sbom();
        parentSBOM.setBuildId("ARYT3LBXDVYAC");
        parentSBOM.setId(416640206274228224L);
        parentSBOM.setStatus(SbomStatus.READY);
        parentSBOM.setType(SbomType.BUILD_TIME);
        parentSBOM.setGenerationTime(Instant.now());
        parentSBOM.setSbom(SbomUtils.toJsonNode(bom));
        parentSBOM.setGenerator(GeneratorType.MAVEN_CYCLONEDX);
        parentSBOM.setParentSbom(null);
        return parentSBOM;
    }

    private Sbom createEnrichedSBOM(Sbom parentSbom) throws IOException {
        Bom bom = SbomUtils.fromPath(sbomPath("processed-default.json"));

        // Not setting rootPurl, as it will be set by PrePersist
        Sbom enrichedSBOM = new Sbom();
        enrichedSBOM.setBuildId("ARYT3LBXDVYAC");
        enrichedSBOM.setId(416640206274228225L);
        enrichedSBOM.setStatus(SbomStatus.READY);
        enrichedSBOM.setType(SbomType.BUILD_TIME);
        enrichedSBOM.setGenerationTime(Instant.now());
        enrichedSBOM.setSbom(SbomUtils.toJsonNode(bom));
        enrichedSBOM.setGenerator(parentSbom.getGenerator());
        enrichedSBOM.setProcessors(Arrays.asList(ProcessorType.DEFAULT).stream().collect(Collectors.toSet()));
        enrichedSBOM.setParentSbom(parentSbom);
        return enrichedSBOM;
    }

    @PostConstruct
    public void init() throws Exception {
        Sbom parentSBOM = createParentSBOM();
        parentSBOM = sbomRepository.saveSbom(parentSBOM);

        Sbom enrichedSBOM = createEnrichedSBOM(parentSBOM);
        sbomRepository.saveSbom(enrichedSBOM);
    }

    @Test
    public void testNonNullRootComponents() {
        String rsqlQuery = "buildId=eq=ARYT3LBXDVYAC;generator==MAVEN_CYCLONEDX;processors=isnull=true";
        Sbom baseSBOM = sbomRepository.searchByQuery(0, 1, rsqlQuery).get(0);

        assertNotNull(baseSBOM.getRootPurl());
        assertEquals(
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom",
                baseSBOM.getRootPurl());

        // TODO
        // Sbom enrichedSbom = sbomRepository
        // .getSbom("ARYT3LBXDVYAC", GeneratorImplementation.CYCLONEDX, ProcessorImplementation.DEFAULT);
        // assertNotNull(enrichedSbom.getRootPurl());
        // assertEquals(
        // "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-parent@1.1.0.redhat-00008?type=pom",
        // enrichedSbom.getRootPurl());
    }

    @Test
    public void testGetBaseSbom() throws JsonProcessingException, JsonMappingException {
        String rsqlQuery = "buildId=eq=ARYT3LBXDVYAC;generator==MAVEN_CYCLONEDX;processors=isnull=true";
        Sbom baseSBOM = sbomRepository.searchByQuery(0, 1, rsqlQuery).get(0);
        Bom bom = baseSBOM.getCycloneDxBom();

        assertEquals(416640206274228224L, baseSBOM.getId());
        assertEquals("ARYT3LBXDVYAC", baseSBOM.getBuildId());
        assertEquals(GeneratorType.MAVEN_CYCLONEDX, baseSBOM.getGenerator());
        assertEquals(SbomType.BUILD_TIME, baseSBOM.getType());
        assertEquals("CycloneDX", bom.getBomFormat());
        Component firstComponent = bom.getComponents().get(0);
        assertEquals("microprofile-graphql-spec", firstComponent.getName());
        assertEquals(
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-spec@1.1.0.redhat-00008?type=pom",
                firstComponent.getPurl());

        Set<ConstraintViolation<Sbom>> violations = validator.validate(baseSBOM);
        if (!violations.isEmpty()) {
            Log.error(
                    "violations: " + violations.stream()
                            .map(e -> e.getMessage().toString())
                            .collect(Collectors.joining("\n\t")));
            fail("Validation errors on the baseSBOM entity should be empty!");
        }
    }

    @Test
    public void testFindByIdSbom() {
        Sbom sbom = sbomRepository.findById(416640206274228224L);
        Bom bom = sbom.getCycloneDxBom();

        assertEquals(416640206274228224L, sbom.getId());
        assertEquals("ARYT3LBXDVYAC", sbom.getBuildId());
        assertEquals(GeneratorType.MAVEN_CYCLONEDX, sbom.getGenerator());
        assertEquals(SbomType.BUILD_TIME, sbom.getType());
        assertEquals(0, sbom.getProcessors().size());
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
    public void testDeleteSboms() throws Exception {

        String buildId = "ACACACACACAC";

        Sbom parentSBOM = createParentSBOM();
        parentSBOM.setId(13L);
        parentSBOM.setBuildId(buildId);
        parentSBOM = sbomRepository.saveSbom(parentSBOM);

        Sbom enrichedSBOM = createEnrichedSBOM(parentSBOM);
        enrichedSBOM.setId(1300L);
        enrichedSBOM.setBuildId(buildId);
        sbomRepository.saveSbom(enrichedSBOM);

        String rsqlQuery = "buildId=eq=" + buildId;
        List<Sbom> sbomsAfterInsert = sbomRepository.searchByQuery(0, 10, rsqlQuery);
        assertEquals(2, sbomsAfterInsert.size());

        sbomRepository.deleteByBuildId(buildId);

        List<Sbom> sbomsAfterDelete = sbomRepository.searchByQuery(0, 10, rsqlQuery);
        assertEquals(0, sbomsAfterDelete.size());
    }

    @Test
    @Disabled("Use case doesn't exist yet")
    public void testGetEnrichedSbom() throws JsonProcessingException, JsonMappingException {
        String rsqlQuery = "buildId=eq=ARYT3LBXDVYAC;generator==MAVEN_CYCLONEDX;processors=isnull=true";
        Sbom enrichedSbom = sbomRepository.searchByQuery(0, 1, rsqlQuery).get(0);
        // Sbom enrichedSbom = sbomRepository
        // .getSbom("ARYT3LBXDVYAC", GeneratorImplementation.CYCLONEDX, ProcessorImplementation.DEFAULT);
        Bom bom = enrichedSbom.getCycloneDxBom();

        assertEquals(416640206274228225L, enrichedSbom.getId());
        assertEquals("ARYT3LBXDVYAC", enrichedSbom.getBuildId());
        assertEquals(GeneratorType.MAVEN_CYCLONEDX, enrichedSbom.getGenerator());
        assertEquals(Arrays.asList(ProcessorType.DEFAULT), enrichedSbom.getProcessors());
        assertEquals(SbomType.BUILD_TIME, enrichedSbom.getType());
        assertEquals("CycloneDX", bom.getBomFormat());
        Component firstComponent = bom.getComponents().get(0);
        assertEquals("microprofile-graphql-spec", firstComponent.getName());
        assertEquals(
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-spec@1.1.0.redhat-00008?type=pom",
                firstComponent.getPurl());

        Set<ConstraintViolation<Sbom>> violations = validator.validate(enrichedSbom);
        if (!violations.isEmpty()) {
            Log.error(
                    "violations: " + violations.stream()
                            .map(e -> e.getMessage().toString())
                            .collect(Collectors.joining("\n\t")));
            fail("Validation errors on the baseSBOM entity should be empty!");
        }

        Sbom parentSBOM = enrichedSbom.getParentSbom();
        Bom parentBom = parentSBOM.getCycloneDxBom();

        assertEquals(416640206274228224L, parentSBOM.getId());
        assertEquals("ARYT3LBXDVYAC", parentSBOM.getBuildId());
        assertEquals(GeneratorType.MAVEN_CYCLONEDX, parentSBOM.getGenerator());
        assertEquals(SbomType.BUILD_TIME, parentSBOM.getType());
        assertNull(parentSBOM.getProcessors());
        assertEquals("CycloneDX", parentBom.getBomFormat());
        Component firstParentComponent = parentBom.getComponents().get(0);
        assertEquals("microprofile-graphql-spec", firstParentComponent.getName());
        assertEquals(
                "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-spec@1.1.0.redhat-00008?type=pom",
                firstParentComponent.getPurl());

    }

}
