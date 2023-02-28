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

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.json.stream.JsonParsingException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redhat.sbomer.model.BaseSBOM;
import org.redhat.sbomer.repositories.BaseSBOMRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import groovy.util.logging.Slf4j;
import io.quarkus.arc.Priority;
import io.quarkus.logging.Log;

@Priority(1)
@Alternative
@ApplicationScoped
@QuarkusTransactionalTest
@Slf4j
public class TestBaseSBOMRepository extends BaseSBOMRepository {

    private ObjectMapper mapper = new ObjectMapper();

    private BaseSBOM createBaseSBOM() throws IOException {
        String bom = TestResources.asString("sboms/sbom-valid.json");
        JsonNode sbom = mapper.readTree(bom);
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
        Assertions.assertEquals(416640206274228224L, baseSBOM.getId());
        Assertions.assertEquals("ARYT3LBXDVYAC", baseSBOM.getBuildId());
        JsonNode sbom = mapper.readTree(baseSBOM.getSbom().asText());

        Assertions.assertEquals("CycloneDX", sbom.get("bomFormat").textValue());
        Assertions.assertEquals("CycloneDX", sbom.path("bomFormat").textValue());
        Assertions.assertEquals(true, sbom.path("components").isArray());
        JsonNode firstComponent = sbom.path("components").get(0);
        Assertions.assertEquals("jcommander", firstComponent.path("name").textValue());
        Assertions
                .assertEquals("pkg:maven/com.beust/jcommander@1.72?type=jar", firstComponent.path("purl").textValue());
    }

}