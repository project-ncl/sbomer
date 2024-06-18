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
package org.jboss.sbomer.cli.test.integ.feature.sbom.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.DeliverableAnalyzerOperation;
import org.jboss.pnc.dto.ProductMilestone;
import org.jboss.pnc.dto.ProductVersion;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.pnc.dto.response.AnalyzedArtifact;
import org.jboss.sbomer.cli.feature.sbom.service.PncService;
import org.jboss.sbomer.cli.test.utils.PncWireMock;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@QuarkusTest
@QuarkusTestResource(PncWireMock.class)
class PncServiceIT {

    @Inject
    PncService service;

    @Test
    void testFetchArtifact() throws Exception {
        log.info("testFetchArtifact ...");
        Artifact fromPNC = service.getArtifact(
                "pkg:maven/org.jboss.logging/commons-logging-jboss-logging@1.0.0.Final-redhat-1?type=jar",
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        assertNotNull(fromPNC);
        assertEquals("312123", fromPNC.getId());
    }

    @Test
    void testFetchNonExistingArtifact() throws Exception {
        assertNull(service.getArtifact("purlnonexisting", Optional.empty(), Optional.empty(), Optional.empty()));
    }

    @Test
    void testGetProductVersionMissingBuild() {
        assertEquals(Collections.emptyList(), service.getProductVersions("NOTEXISTING"));
    }

    @Test
    void testGetProductVersion() {
        List<ProductVersionRef> productVersions = service.getProductVersions("ARYT3LBXDVYAC");

        assertNotNull(productVersions);
        assertFalse(productVersions.isEmpty());
        assertEquals(1, productVersions.size());
        assertEquals("179", productVersions.get(0).getId());
        assertEquals("1.0", productVersions.get(0).getVersion());
    }

    /**
     * This tests a case where the productVersion in the build config is null. If this is the case we try to retrieve
     * the productVersion from a groupConfig the build config is assigned to (if any).
     */
    @Test
    void testGetProductVersionFromGroupConfig() {
        List<ProductVersionRef> productVersions = service.getProductVersions("BUILDWITHGROUPSCONFIGPRODUCT");

        assertNotNull(productVersions);
        assertFalse(productVersions.isEmpty());
        assertEquals(1, productVersions.size());
        assertEquals("483", productVersions.get(0).getId());
        assertEquals("3.2", productVersions.get(0).getVersion());
    }

    @Test
    void testFetchDuplicatedArtifactNoSha256() throws Exception {
        log.info("testFetchDuplicatedArtifact ...");
        String purl = "pkg:maven/org.jboss.logging/commons-logging-jboss-logging@13.0.0.Final-redhat-1?type=jar";

        Artifact artifact = service.getArtifact(purl, Optional.empty(), Optional.empty(), Optional.empty());

        assertEquals("cccc", artifact.getSha256());
        assertEquals("412123", artifact.getId());
    }

    @Test
    void testFetchProductVersion() throws Exception {
        log.info("testFetchProductVersion ...");
        ProductVersion productVersion = service.getProductVersion("316");
        assertNotNull(productVersion);
        assertEquals("316", productVersion.getId());
        assertEquals("7.11", productVersion.getVersion());
        assertEquals(15, productVersion.getProductMilestones().size());
    }

    @Test
    void testFetchProductMilestone() throws Exception {
        log.info("testFetchProductMilestone ...");
        ProductMilestone productMilestone = service.getMilestone("2655");
        assertNotNull(productMilestone);
        assertEquals("2655", productMilestone.getId());
        assertEquals("7.11.5.CR3", productMilestone.getVersion());
        assertEquals("316", productMilestone.getProductVersion().getId());
        assertEquals("7.11", productMilestone.getProductVersion().getVersion());
    }

    @Test
    void testFetchOperation() throws Exception {
        log.info("testFetchOperation ...");
        DeliverableAnalyzerOperation operation = service.getDeliverableAnalyzerOperation("A5RPHL7Y3AIAA");
        assertNotNull(operation);
        assertEquals("A5RPHL7Y3AIAA", operation.getId());
        assertEquals(2, operation.getParameters().size());
        assertTrue(operation.getParameters().containsValue("https://download.com/my-7.11.5.CR3-bin.zip"));
        assertEquals("2655", operation.getProductMilestone().getId());
    }

    @Test
    void testFetchOperationResults() throws Exception {
        log.info("testFetchOperationResults ...");
        List<AnalyzedArtifact> artifacts = service.getAllAnalyzedArtifacts("A5RPHL7Y3AIAA");
        assertNotNull(artifacts);
        assertEquals(1, artifacts.size());
    }
}
