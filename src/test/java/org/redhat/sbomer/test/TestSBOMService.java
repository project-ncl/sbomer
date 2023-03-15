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
import java.util.Optional;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Property;
import org.jboss.pnc.common.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.redhat.sbomer.dto.BaseSBOM;
import org.redhat.sbomer.dto.response.Page;
import org.redhat.sbomer.service.SBOMService;
import org.redhat.sbomer.test.mock.PncServiceMock;
import org.redhat.sbomer.transformer.PncArtifactsToPropertiesSbomTransformer;
import org.redhat.sbomer.transformer.SbomManipulator;
import org.redhat.sbomer.validation.exceptions.ValidationException;

import com.fasterxml.jackson.databind.JsonNode;

import io.quarkus.test.junit.QuarkusTest;
import lombok.extern.slf4j.Slf4j;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_BUILD_ID;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_BUILD_SYSTEM;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_ORIGIN_URL;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_SCM_URL;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_SCM_REVISION;

import static org.redhat.sbomer.utils.SbomUtils.findComponentWithPurl;
import static org.redhat.sbomer.utils.SbomUtils.findPropertyWithNameInComponent;

@QuarkusTest
@Slf4j
public class TestSBOMService {

    @Inject
    SBOMService sbomService;

    @Inject
    PncServiceMock pncServiceMock;

    @Inject
    SbomManipulator sbomManipulator;

    @Inject
    PncArtifactsToPropertiesSbomTransformer artifactsToPropertiesSbomTransformer;

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

        BaseSBOM foundSbom = null;
        Iterator<BaseSBOM> contentIterator = page.getContent().iterator();
        while (contentIterator.hasNext()) {
            BaseSBOM sbom = contentIterator.next();
            if (sbom.getBuildId().equals(INITIAL_BUILD_ID)) {
                foundSbom = sbom;
                break;
            }
        }

        assertNotNull(foundSbom);
    }

    @Test
    public void testBaseSbomNotFound() throws IOException {
        log.info("testBaseSbomNotFound ...");
        try {
            sbomService.getBaseSbom("I_DO_NOT_EXIST");
            fail("It should have thrown a 404 exception");
        } catch (NotFoundException nfe) {
        }
    }

    @Test
    public void testFetchArtifact() throws IOException {
        log.info("testFetchArtifact ...");
        try {
            String purlFromDB = "pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar";
            org.redhat.sbomer.dto.ArtifactCache fromDB = sbomService.fetchArtifact(purlFromDB);
            assertNotNull(fromDB);
        } catch (NotFoundException nfe) {
            fail("It should have not thrown a not found exception", nfe);
        }

        try {
            String purlFromPNC = "pkg:maven/com.vaadin.external.google/android-json@0.0.20131108.vaadin1?type=jar";
            org.redhat.sbomer.dto.ArtifactCache fromPNC = sbomService.fetchArtifact(purlFromPNC);
            assertNotNull(fromPNC);
        } catch (NotFoundException nfe) {
            fail("It should have not thrown a not found exception", nfe);
        }

        try {
            String purlNotExisting = "pkg:maven/i.do.not/exist@0.0.0?type=jar";
            sbomService.fetchArtifact(purlNotExisting);
            fail("It should have thrown a not found exception");
        } catch (NotFoundException nfe) {
        }
    }

    @Test
    public void testManipulateSBOMAddingProperties() {
        log.info("testManipulateSBOMAddingProperties ...");

        try {
            BaseSBOM baseSBOM = sbomService.getBaseSbom(INITIAL_BUILD_ID);
            Bom bom = new org.cyclonedx.parsers.JsonParser().parse(baseSBOM.getBom().textValue().getBytes());

            Bom modifiedBom = sbomManipulator.addTransformer(artifactsToPropertiesSbomTransformer).runTransformers(bom);

            Component notFoundInCacheNorPNCComponent = findComponentWithPurl(
                    "pkg:maven/commons-io/commons-io@2.6.0.redhat-00001?type=jar",
                    modifiedBom).get();
            Optional<Property> build = findPropertyWithNameInComponent(
                    SBOM_RED_HAT_BUILD_ID,
                    notFoundInCacheNorPNCComponent);
            assertEquals("32374", build.get().getValue());
            Optional<Property> buildSystem = findPropertyWithNameInComponent(
                    SBOM_RED_HAT_BUILD_SYSTEM,
                    notFoundInCacheNorPNCComponent);
            assertEquals("PNC", buildSystem.get().getValue());
            Optional<Property> environmentImage = findPropertyWithNameInComponent(
                    SBOM_RED_HAT_ENVIRONMENT_IMAGE,
                    notFoundInCacheNorPNCComponent);
            assertFalse(environmentImage.isPresent());
            Optional<Property> originUrl = findPropertyWithNameInComponent(
                    SBOM_RED_HAT_ORIGIN_URL,
                    notFoundInCacheNorPNCComponent);
            assertFalse(originUrl.isPresent());
            Optional<Property> scmUrl = findPropertyWithNameInComponent(
                    SBOM_RED_HAT_SCM_URL,
                    notFoundInCacheNorPNCComponent);
            assertFalse(scmUrl.isPresent());
            Optional<Property> scmRevision = findPropertyWithNameInComponent(
                    SBOM_RED_HAT_SCM_REVISION,
                    notFoundInCacheNorPNCComponent);
            assertFalse(scmRevision.isPresent());

            Component foundInCacheComponent = findComponentWithPurl(
                    "pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar",
                    modifiedBom).get();
            build = findPropertyWithNameInComponent(SBOM_RED_HAT_BUILD_ID, foundInCacheComponent);
            assertEquals("AVOBVY3O23YAA", build.get().getValue());
            buildSystem = findPropertyWithNameInComponent(SBOM_RED_HAT_BUILD_SYSTEM, foundInCacheComponent);
            assertEquals("PNC", buildSystem.get().getValue());
            environmentImage = findPropertyWithNameInComponent(SBOM_RED_HAT_ENVIRONMENT_IMAGE, foundInCacheComponent);
            assertEquals(
                    "quay.io/rh-newcastle/builder-rhel-8-j8-mvn3.5.4-netty-tcnative:1.0.2",
                    environmentImage.get().getValue());
            originUrl = findPropertyWithNameInComponent(SBOM_RED_HAT_ORIGIN_URL, foundInCacheComponent);
            assertEquals(
                    "https://maven.repository.redhat.com/com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar",
                    originUrl.get().getValue());
            scmUrl = findPropertyWithNameInComponent(SBOM_RED_HAT_SCM_URL, foundInCacheComponent);
            assertEquals("https://code.engineering.redhat.com/gerrit/hyperxpro/Brotli4j.git", scmUrl.get().getValue());
            scmRevision = findPropertyWithNameInComponent(SBOM_RED_HAT_SCM_REVISION, foundInCacheComponent);
            assertEquals("6f25bf15308ee95ef5a9783412be2b0557c9046d", scmRevision.get().getValue());

            Component foundInPNCComponent = findComponentWithPurl(
                    "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-api@1.1.0.redhat-00008?type=jar",
                    modifiedBom).get();
            build = findPropertyWithNameInComponent(SBOM_RED_HAT_BUILD_ID, foundInPNCComponent);
            assertEquals("ARYT3LBXDVYAC", build.get().getValue());
            buildSystem = findPropertyWithNameInComponent(SBOM_RED_HAT_BUILD_SYSTEM, foundInPNCComponent);
            assertEquals("PNC", buildSystem.get().getValue());
            environmentImage = findPropertyWithNameInComponent(SBOM_RED_HAT_ENVIRONMENT_IMAGE, foundInPNCComponent);
            assertEquals("quay.io/rh-newcastle/builder-rhel-7-j11-mvn3.6.8:1.0.0", environmentImage.get().getValue());
            originUrl = findPropertyWithNameInComponent(SBOM_RED_HAT_ORIGIN_URL, foundInPNCComponent);
            assertEquals(
                    "https://maven.repository.redhat.com/ga/com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar",
                    originUrl.get().getValue());
            scmUrl = findPropertyWithNameInComponent(SBOM_RED_HAT_SCM_URL, foundInPNCComponent);
            assertEquals("https://code.engineering.redhat.com/gerrit/hyperxpro/Brotli4j.git", scmUrl.get().getValue());
            scmRevision = findPropertyWithNameInComponent(SBOM_RED_HAT_SCM_REVISION, foundInPNCComponent);
            assertEquals("6f25bf15308ee95ef5a9783412be2b0557c9046d", scmRevision.get().getValue());

            sbomService.updateBom(Long.valueOf(baseSBOM.getId()), modifiedBom);

            // Now getting again from DB and re-run all the previous checks
            BaseSBOM updatedBaseSBOM = sbomService.getBaseSbom(INITIAL_BUILD_ID);
            Bom bomFromDB = new org.cyclonedx.parsers.JsonParser()
                    .parse(updatedBaseSBOM.getBom().textValue().getBytes());

            notFoundInCacheNorPNCComponent = findComponentWithPurl(
                    "pkg:maven/commons-io/commons-io@2.6.0.redhat-00001?type=jar",
                    bomFromDB).get();
            build = findPropertyWithNameInComponent(SBOM_RED_HAT_BUILD_ID, notFoundInCacheNorPNCComponent);
            assertEquals("32374", build.get().getValue());
            buildSystem = findPropertyWithNameInComponent(SBOM_RED_HAT_BUILD_SYSTEM, notFoundInCacheNorPNCComponent);
            assertEquals("PNC", buildSystem.get().getValue());
            environmentImage = findPropertyWithNameInComponent(
                    SBOM_RED_HAT_ENVIRONMENT_IMAGE,
                    notFoundInCacheNorPNCComponent);
            assertFalse(environmentImage.isPresent());
            originUrl = findPropertyWithNameInComponent(SBOM_RED_HAT_ORIGIN_URL, notFoundInCacheNorPNCComponent);
            assertFalse(originUrl.isPresent());
            scmUrl = findPropertyWithNameInComponent(SBOM_RED_HAT_SCM_URL, notFoundInCacheNorPNCComponent);
            assertFalse(scmUrl.isPresent());
            scmRevision = findPropertyWithNameInComponent(SBOM_RED_HAT_SCM_REVISION, notFoundInCacheNorPNCComponent);
            assertFalse(scmRevision.isPresent());

            foundInCacheComponent = findComponentWithPurl(
                    "pkg:maven/com.aayushatharva.brotli4j/brotli4j@1.8.0.redhat-00003?type=jar",
                    bomFromDB).get();
            build = findPropertyWithNameInComponent(SBOM_RED_HAT_BUILD_ID, foundInCacheComponent);
            assertEquals("AVOBVY3O23YAA", build.get().getValue());
            buildSystem = findPropertyWithNameInComponent(SBOM_RED_HAT_BUILD_SYSTEM, foundInCacheComponent);
            assertEquals("PNC", buildSystem.get().getValue());
            environmentImage = findPropertyWithNameInComponent(SBOM_RED_HAT_ENVIRONMENT_IMAGE, foundInCacheComponent);
            assertEquals(
                    "quay.io/rh-newcastle/builder-rhel-8-j8-mvn3.5.4-netty-tcnative:1.0.2",
                    environmentImage.get().getValue());
            originUrl = findPropertyWithNameInComponent(SBOM_RED_HAT_ORIGIN_URL, foundInCacheComponent);
            assertEquals(
                    "https://maven.repository.redhat.com/com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar",
                    originUrl.get().getValue());
            scmUrl = findPropertyWithNameInComponent(SBOM_RED_HAT_SCM_URL, foundInCacheComponent);
            assertEquals("https://code.engineering.redhat.com/gerrit/hyperxpro/Brotli4j.git", scmUrl.get().getValue());
            scmRevision = findPropertyWithNameInComponent(SBOM_RED_HAT_SCM_REVISION, foundInCacheComponent);
            assertEquals("6f25bf15308ee95ef5a9783412be2b0557c9046d", scmRevision.get().getValue());

            foundInPNCComponent = findComponentWithPurl(
                    "pkg:maven/org.eclipse.microprofile.graphql/microprofile-graphql-api@1.1.0.redhat-00008?type=jar",
                    bomFromDB).get();
            build = findPropertyWithNameInComponent(SBOM_RED_HAT_BUILD_ID, foundInPNCComponent);
            assertEquals("ARYT3LBXDVYAC", build.get().getValue());
            buildSystem = findPropertyWithNameInComponent(SBOM_RED_HAT_BUILD_SYSTEM, foundInPNCComponent);
            assertEquals("PNC", buildSystem.get().getValue());
            environmentImage = findPropertyWithNameInComponent(SBOM_RED_HAT_ENVIRONMENT_IMAGE, foundInPNCComponent);
            assertEquals("quay.io/rh-newcastle/builder-rhel-7-j11-mvn3.6.8:1.0.0", environmentImage.get().getValue());
            originUrl = findPropertyWithNameInComponent(SBOM_RED_HAT_ORIGIN_URL, foundInPNCComponent);
            assertEquals(
                    "https://maven.repository.redhat.com/ga/com/aayushatharva/brotli4j/brotli4j/1.8.0.redhat-00003/brotli4j-1.8.0.redhat-00003.jar",
                    originUrl.get().getValue());
            scmUrl = findPropertyWithNameInComponent(SBOM_RED_HAT_SCM_URL, foundInPNCComponent);
            assertEquals("https://code.engineering.redhat.com/gerrit/hyperxpro/Brotli4j.git", scmUrl.get().getValue());
            scmRevision = findPropertyWithNameInComponent(SBOM_RED_HAT_SCM_REVISION, foundInPNCComponent);
            assertEquals("6f25bf15308ee95ef5a9783412be2b0557c9046d", scmRevision.get().getValue());

        } catch (NotFoundException nfe) {
            fail("It should not have thrown a 404 exception", nfe);
        } catch (ValidationException ve) {
            fail("It should not have thrown a validation exception", ve);
        } catch (ParseException pExc) {
            fail("It should not have thrown a parse exception", pExc);
        }
    }

}
