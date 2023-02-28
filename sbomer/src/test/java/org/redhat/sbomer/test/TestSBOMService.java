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
import java.util.Iterator;
import java.util.List;

import javax.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redhat.sbomer.dto.BaseSBOM;
import org.redhat.sbomer.dto.response.Page;
import org.redhat.sbomer.service.SBOMService;

import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;

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
        Assertions.assertNotNull(baseSBOM);
    }

    @Test
    public void testListBaseSboms() throws IOException {
        log.info("testListBaseSboms ...");

        Page<BaseSBOM> page = sbomService.listBaseSboms(0, 50);
        Assertions.assertEquals(0, page.getPageIndex());
        Assertions.assertEquals(50, page.getPageSize());
        Assertions.assertTrue(page.getTotalHits() > 0);
        Assertions.assertEquals(1, page.getTotalPages());
        Assertions.assertTrue(page.getContent().size() > 0);

        boolean found = false;
        Iterator<BaseSBOM> contentIterator = page.getContent().iterator();
        while (contentIterator.hasNext()) {
            BaseSBOM sbom = contentIterator.next();
            found = sbom.getBuildId().equals(INITIAL_BUILD_ID);
            if (found) {
                return;
            }
        }
        Assertions.assertTrue(found);
    }
}
