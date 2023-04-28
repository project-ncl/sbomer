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
package org.jboss.sbomer.core.test.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.inject.Inject;

import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.service.PncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@QuarkusTest
@QuarkusTestResource(PncWireMock.class)
public class PncServiceTest {

    @Inject
    PncService service;

    @Test
    void testFetchArtifact() throws Exception {
        log.info("testFetchArtifact ...");
        Artifact fromPNC = service
                .getArtifact("pkg:maven/org.jboss.logging/commons-logging-jboss-logging@1.0.0.Final-redhat-1?type=jar");
        assertNotNull(fromPNC);
        assertEquals("312123", fromPNC.getId());
    }

    @Test
    void testFetchNonExistingArtifact() throws Exception {
        ApplicationException ex = Assertions.assertThrows(ApplicationException.class, () -> {
            service.getArtifact("purlnonexisting");
        });

        assertEquals("Artifact with purl 'purlnonexisting' was not found in PNC", ex.getMessage());
    }

    @Test
    void testGetProductVersionMissingBuild() {
        assertNull(service.getProductVersion("NOTEXISTING"));
    }

    @Test
    void testGetProductVersion() {
        ProductVersionRef productVersionRef = service.getProductVersion("ARYT3LBXDVYAC");

        assertNotNull(productVersionRef);
        assertEquals("179", productVersionRef.getId());
        assertEquals("1.0", productVersionRef.getVersion());
    }
}
