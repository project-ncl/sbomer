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
import org.redhat.sbomer.model.BaseSBOM;
import org.redhat.sbomer.repositories.BaseSBOMRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.arc.Priority;
import io.quarkus.logging.Log;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

@Priority(1)
@Alternative
@ApplicationScoped
@QuarkusTransactionalTest
public class TestBaseSBOMRepository extends BaseSBOMRepository {

    @Inject
    Validator validator;

    private BaseSBOM createBaseSBOM() throws IOException {
        String bom = TestResources.asString("sboms/sbom-valid.json");
        JsonNode sbom = JsonUtils.fromJson(bom, JsonNode.class);
        BaseSBOM baseSBOM = new BaseSBOM();
        baseSBOM.setBuildId("ARYT3LBXDVYAC");
        baseSBOM.setId(416640206274228224L);
        baseSBOM.setGenerationTime(Instant.now());
        baseSBOM.setSbom(sbom);

        return baseSBOM;
    }

    @PostConstruct
    public void init() {
        try {
            BaseSBOM baseSBOM = createBaseSBOM();
            saveBom(baseSBOM);
        } catch (IOException exc) {
            Log.error("Failed to persist new base SBOM", exc);
        }
    }

    @Test
    public void testGetBaseSbom() throws JsonProcessingException, JsonMappingException {
        BaseSBOM baseSBOM = getBaseSbom("ARYT3LBXDVYAC");
        Bom bom = baseSBOM.getCycloneDxBom();

        assertEquals(416640206274228224L, baseSBOM.getId());
        assertEquals("ARYT3LBXDVYAC", baseSBOM.getBuildId());
        assertEquals("CycloneDX", bom.getBomFormat());
        Component firstComponent = bom.getComponents().get(0);
        assertEquals("jcommander", firstComponent.getName());
        assertEquals("pkg:maven/com.beust/jcommander@1.72?type=jar", firstComponent.getPurl());

        Set<ConstraintViolation<BaseSBOM>> violations = validator.validate(baseSBOM);
        if (!violations.isEmpty()) {
            Log.error(
                    "violations: " + violations.stream()
                            .map(e -> e.getMessage().toString())
                            .collect(Collectors.joining("\n\t")));
            fail("Validation errors on the baseSBOM entity should be empty!");
        }
    }

    @Test
    public void testFindByIdBaseSbom() {
        BaseSBOM baseSBOM = findById(416640206274228224L);
        Bom bom = baseSBOM.getCycloneDxBom();

        assertEquals(416640206274228224L, baseSBOM.getId());
        assertEquals("ARYT3LBXDVYAC", baseSBOM.getBuildId());
        assertEquals("CycloneDX", bom.getBomFormat());
        Component firstComponent = bom.getComponents().get(0);
        assertEquals("jcommander", firstComponent.getName());
        assertEquals("pkg:maven/com.beust/jcommander@1.72?type=jar", firstComponent.getPurl());

        Set<ConstraintViolation<BaseSBOM>> violations = validator.validate(baseSBOM);
        if (!violations.isEmpty()) {
            Log.error(
                    "violations: " + violations.stream()
                            .map(e -> e.getMessage().toString())
                            .collect(Collectors.joining("\n\t")));
            fail("Validation errors on the baseSBOM entity should be empty!");
        }
    }

}