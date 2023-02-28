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
import java.util.Iterator;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.junit.jupiter.api.Test;
import org.redhat.sbomer.dto.BaseSBOM;
import org.redhat.sbomer.dto.response.Page;
import org.redhat.sbomer.service.SBOMService;

import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@QuarkusTest
@Slf4j
public class TestSBOMService {

    @Inject
    SBOMService sbomService;

    private static final String INITIAL_BUILD_ID = "ARYT3LBXDVYAC";

    @Test
    public void testGetBaseSbom() throws IOException {
        log.info("testGetBaseSbom ...");
        BaseSBOM baseSBOM = sbomService.getBaseSbom(INITIAL_BUILD_ID);
        assertNotNull(baseSBOM);
    }

    @Test
    public void testListBaseSboms() throws IOException {
        log.info("testListBaseSboms ...");

        Page<BaseSBOM> page = sbomService.listBaseSboms(0, 50);
        assertEquals(0, page.getPageIndex());
        assertEquals(50, page.getPageSize());
        assertTrue(page.getTotalHits() > 0);
        assertEquals(1, page.getTotalPages());
        assertTrue(page.getContent().size() > 0);

        boolean found = false;
        Iterator<BaseSBOM> contentIterator = page.getContent().iterator();
        while (contentIterator.hasNext()) {
            BaseSBOM sbom = contentIterator.next();
            found = sbom.getBuildId().equals(INITIAL_BUILD_ID);
            if (found) {
                return;
            }
        }
        assertTrue(found);
    }

    @Test
    public void testBaseSbomNotFound() throws IOException {
        log.info("testBaseSbomNotFound ...");
        try {
            BaseSBOM baseSBOM = sbomService.getBaseSbom("I_DO_NOT_EXIST");
            fail("It should have thrown a 404 exception");
        } catch (NotFoundException nfe) {
        }
    }
}
