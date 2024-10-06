/*
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
package org.jboss.sbomer.service.test.unit.feature.sbom.errata;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataType;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.ErrataNotificationHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

class ErrataNotificationHandlerTest {

    static class ErrataNotificationHandlerAlt extends ErrataNotificationHandler {
        @Override
        public void handle(String message) throws JsonProcessingException {
            super.handle(message);
        }
    }

    ErrataNotificationHandlerAlt errataNotificationHandler;

    ErrataClient errataClient = mock(ErrataClient.class);

    @BeforeEach
    void beforeEach() {
        errataNotificationHandler = new ErrataNotificationHandlerAlt();
        errataNotificationHandler.setErrataClient(errataClient);
    }

    private Errata createErrata(Long id) {
        Errata.ErrataProduct innerProduct = Errata.ErrataProduct.builder()
                .withId(153L)
                .withName("Red Hat build of Quarkus")
                .withShortName("RHBQ")
                .build();
        Errata.Rhba rhba = Errata.Rhba.builder()
                .withId(id)
                .withFulladvisory("RHBA-2041:7158-01")
                .withSynopsis("updated q/m container image")
                .withStatus(ErrataStatus.SHIPPED_LIVE)
                .withBrew(true)
                .withGroupId(2227L)
                .withProduct(innerProduct)
                .build();
        Errata.Content content = Errata.Content.builder()
                .withId(136810L)
                .withCve("")
                .withDescription(
                        "The q/m container image has been updated to address the following security advisory: RHSA-2041:6975 (see References)\n\nUsers of q/m container images are advised to upgrade to these updated images, which contain backported patches to correct these security issues, fix these bugs and add these enhancements. Users of these images are also encouraged to rebuild all container images that depend on these images.\n\nYou can find images updated by this advisory in Red Hat Container Catalog (see References).")
                .withErrataId(id)
                .withProductVersionText("")
                .withSolution("The container image provided by this update can be downloaded.")
                .withTopic("Updated q/m container image is now available.")
                .withUpdatedAt(ZonedDateTime.of(2041, 9, 26, 8, 23, 7, 0, ZoneOffset.UTC).toInstant())
                .withNotes(
                        "{\r\n  \"manifest\": {\r\n    \"refs\": [\r\n      {\r\n        \"type\": \"purl\",\r\n        \"uri\": \"pkg:oci/q-m@sha256%3A?os=linux&arch=arm64&tag=13.12345\"\r\n      },\r\n      {\r\n        \"type\": \"purl\",\r\n        \"uri\": \"pkg:oci/q-m@sha256%3A6cf157?os=linux&arch=amd64&tag=13.12345\"\r\n      }\r\n    ]\r\n  }\r\n}")
                .build();
        Errata.WrappedContent wrappedContent = Errata.WrappedContent.builder().withContent(content).build();
        Errata.WrappedErrata wrappedErrata = Errata.WrappedErrata.builder().withRhba(rhba).build();
        Errata errata = Errata.builder()
                .withContent(wrappedContent)
                .withOriginalType(ErrataType.RHBA)
                .withErrata(wrappedErrata)
                .build();
        return errata;
    }

    private ErrataRelease createErrataRelease(Long id) {
        ErrataRelease.ErrataAttributes attributes = ErrataRelease.ErrataAttributes.builder()
                .withName("RHEL-8-RHBQ-3.8")
                .withDescription("Red Hat build of Quarkus 3.8 on RHEL 8")
                .build();
        ErrataRelease.ErrataProduct product = ErrataRelease.ErrataProduct.builder()
                .withId(153L)
                .withShortName("RHBQ")
                .build();
        ErrataRelease.ErrataProductVersion productVersion = ErrataRelease.ErrataProductVersion.builder()
                .withId(2166L)
                .withName("RHEL-8-RHBQ-3.8")
                .build();
        ErrataRelease.ErrataProductVersion[] productVersions = { productVersion };
        ErrataRelease.Relationships relationships = ErrataRelease.Relationships.builder()
                .withProduct(product)
                .withProductVersions(productVersions)
                .build();
        ErrataRelease release = ErrataRelease.builder()
                .withData(
                        ErrataRelease.ReleaseData.builder()
                                .withId(id)
                                .withType("releases")
                                .withAttributes(attributes)
                                .withRelationships(relationships)
                                .build())
                .build();
        return release;
    }

    @Test
    void testGetErrata() throws IOException {
        String umbErrataStatusChangeMsg = TestResources.asString("errata/umb/errata_status_change.json");
        Errata errata = createErrata(139230L);
        ErrataRelease release = createErrataRelease(2227L);

        when(errataClient.getErratum("139230")).thenReturn(errata);
        when(errataClient.getReleases("2227")).thenReturn(release);

        errataNotificationHandler.handle(umbErrataStatusChangeMsg);
    }

}
