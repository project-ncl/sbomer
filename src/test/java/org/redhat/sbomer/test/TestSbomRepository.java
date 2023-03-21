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
package org.redhat.sbomer.test;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.jboss.pnc.common.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.redhat.sbomer.model.Sbom;
import org.redhat.sbomer.repositories.SbomRepository;
import org.redhat.sbomer.utils.enums.Generators;
import org.redhat.sbomer.utils.enums.Processors;
import org.redhat.sbomer.utils.enums.SbomType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.arc.Priority;
import io.quarkus.logging.Log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNull;

@Priority(1)
@Alternative
@ApplicationScoped
@QuarkusTransactionalTest
public class TestSbomRepository extends SbomRepository {

    @Inject
    Validator validator;

    private Sbom createParentSBOM() throws IOException {
        String bom = TestResources.asString("sboms/sbom-valid-parent.json");
        JsonNode sbom = JsonUtils.fromJson(bom, JsonNode.class);
        Sbom parentSBOM = new Sbom();
        parentSBOM.setBuildId("ARYT3LBXDVYAC");
        parentSBOM.setId(416640206274228224L);
        parentSBOM.setType(SbomType.BUILD_TIME);
        parentSBOM.setGenerationTime(Instant.now());
        parentSBOM.setSbom(sbom);
        parentSBOM.setGenerator(Generators.CYCLONEDX);
        parentSBOM.setParentSbom(null);
        return parentSBOM;
    }

    private Sbom createEnrichedSBOM() throws IOException {
        Sbom parentSbom = createParentSBOM();
        String bom = TestResources.asString("sboms/sbom-valid-enriched-v10.json");
        JsonNode sbom = JsonUtils.fromJson(bom, JsonNode.class);
        Sbom enrichedSBOM = new Sbom();
        enrichedSBOM.setBuildId("ARYT3LBXDVYAC");
        enrichedSBOM.setId(416640206274228225L);
        enrichedSBOM.setType(SbomType.BUILD_TIME);
        enrichedSBOM.setGenerationTime(Instant.now());
        enrichedSBOM.setSbom(sbom);
        enrichedSBOM.setGenerator(parentSbom.getGenerator());
        enrichedSBOM.setProcessor(Processors.SBOM_PROPERTIES);
        enrichedSBOM.setParentSbom(parentSbom);
        return enrichedSBOM;
    }

    @PostConstruct
    public void init() {
        try {
            Sbom parentSBOM = createParentSBOM();
            saveSbom(parentSBOM);

            Sbom enrichedSBOM = createEnrichedSBOM();
            saveSbom(enrichedSBOM);
        } catch (IOException exc) {
            Log.error("Failed to persist parent and enriched SBOMs", exc);
        }
    }

    @Test
    public void testGetBaseSbom() throws JsonProcessingException, JsonMappingException {
        Sbom baseSBOM = getSbom("ARYT3LBXDVYAC", Generators.CYCLONEDX, null);
        Bom bom = baseSBOM.getCycloneDxBom();

        assertEquals(416640206274228224L, baseSBOM.getId());
        assertEquals("ARYT3LBXDVYAC", baseSBOM.getBuildId());
        assertEquals(Generators.CYCLONEDX, baseSBOM.getGenerator());
        assertEquals(SbomType.BUILD_TIME, baseSBOM.getType());
        assertEquals("CycloneDX", bom.getBomFormat());
        Component firstComponent = bom.getComponents().get(0);
        assertEquals("jcommander", firstComponent.getName());
        assertEquals("pkg:maven/com.beust/jcommander@1.72?type=jar", firstComponent.getPurl());

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
        Sbom sbom = findById(416640206274228224L);
        Bom bom = sbom.getCycloneDxBom();

        assertEquals(416640206274228224L, sbom.getId());
        assertEquals("ARYT3LBXDVYAC", sbom.getBuildId());
        assertEquals(Generators.CYCLONEDX, sbom.getGenerator());
        assertEquals(SbomType.BUILD_TIME, sbom.getType());
        assertNull(sbom.getProcessor());
        assertEquals("CycloneDX", bom.getBomFormat());
        Component firstComponent = bom.getComponents().get(0);
        assertEquals("jcommander", firstComponent.getName());
        assertEquals("pkg:maven/com.beust/jcommander@1.72?type=jar", firstComponent.getPurl());

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
    public void testGetEnrichedSbom() throws JsonProcessingException, JsonMappingException {
        Sbom enrichedSbom = getSbom("ARYT3LBXDVYAC", Generators.CYCLONEDX, Processors.SBOM_PROPERTIES);
        Bom bom = enrichedSbom.getCycloneDxBom();

        assertEquals(416640206274228225L, enrichedSbom.getId());
        assertEquals("ARYT3LBXDVYAC", enrichedSbom.getBuildId());
        assertEquals(Generators.CYCLONEDX, enrichedSbom.getGenerator());
        assertEquals(Processors.SBOM_PROPERTIES, enrichedSbom.getProcessor());
        assertEquals(SbomType.BUILD_TIME, enrichedSbom.getType());
        assertEquals("CycloneDX", bom.getBomFormat());
        Component firstComponent = bom.getComponents().get(0);
        assertEquals("cpaas-test-pnc-maven", firstComponent.getName());
        assertEquals("pkg:maven/cpaas.tp/cpaas-test-pnc-maven@1.0.0.redhat-04562?type=jar", firstComponent.getPurl());

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
        assertEquals(Generators.CYCLONEDX, parentSBOM.getGenerator());
        assertEquals(SbomType.BUILD_TIME, parentSBOM.getType());
        assertNull(parentSBOM.getProcessor());
        assertEquals("CycloneDX", parentBom.getBomFormat());
        Component firstParentComponent = parentBom.getComponents().get(0);
        assertEquals("jcommander", firstParentComponent.getName());
        assertEquals("pkg:maven/com.beust/jcommander@1.72?type=jar", firstParentComponent.getPurl());

    }

}
