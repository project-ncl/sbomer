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
package org.jboss.sbomer.cli.test.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.cli.client.SBOMerClient;
import org.jboss.sbomer.cli.model.Sbom;
import org.jboss.sbomer.core.errors.ApiException;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(ServiceWireMock.class)
public class SBOMerClientTest {

    @Inject
    @RestClient
    SBOMerClient client;

    @Test
    void testGetValidSbom() {
        Sbom sbom = client.getById("123");
        assertNotNull(sbom);
        assertEquals(123, sbom.getId());
        assertEquals("BUILD123", sbom.getBuildId());
    }

    @Test
    void testNotFoundSbom() {
        ApiException ex = assertThrows(ApiException.class, () -> {
            client.getById("1234");
        });

        assertEquals(404, ex.getCode());
        assertEquals("Not Found", ex.getMessage());
        assertEquals("cc015e2c-e4e7-11ed-b5ea-0242ac120002", ex.getErrorId());
        assertNull(ex.getErrors());

    }
}
