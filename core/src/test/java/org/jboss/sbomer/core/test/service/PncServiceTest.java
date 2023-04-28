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

import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.dto.Artifact;
import org.jboss.sbomer.core.service.PncService;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@QuarkusTest
public class PncServiceTest {

    @Inject
    PncService service;

    @Test
    void testConfigurationWithCustomCacheUrl() {
        Configuration config = service.getConfiguration();
        assertEquals("localhost/pnc/orch", config.getHost());
        assertEquals("http", config.getProtocol());
    }

    @Test
    public void testFetchArtifact() throws Exception {
        log.info("testFetchArtifact ...");
        String purlFromPNC = "pkg:maven/com.vaadin.external.google/android-json@0.0.20131108.vaadin1?type=jar";
        Artifact fromPNC = service.getArtifact(purlFromPNC);
        assertNotNull(fromPNC);
        assertEquals("MOCKMOCKMOCK1", fromPNC.getBuild().getId());
    }

    @Test
    public void testFetchNonExistingArtifact() throws Exception {

        String purlNotExisting = "pkg:maven/i.do.not/exist@0.0.0?type=jar";
        Artifact missing = service.getArtifact(purlNotExisting);
        assertNull(missing);

    }
}
