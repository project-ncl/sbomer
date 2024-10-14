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

import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.test.TestResources;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.sbom.errata.ErrataClient;
import org.jboss.sbomer.service.feature.sbom.errata.dto.Errata;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataBuildList;
import org.jboss.sbomer.service.feature.sbom.errata.dto.ErrataRelease;
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

        FeatureFlags featureFlags = mock(FeatureFlags.class);
        when(featureFlags.errataIntegrationEnabled()).thenReturn(true);
        errataNotificationHandler.setFeatureFlags(featureFlags);
        errataNotificationHandler.setErrataClient(errataClient);
    }

    @Test
    void testGetErrata() throws IOException {
        String errataJsonString = TestResources.asString("errata/api/erratum.json");
        String releaseJsonString = TestResources.asString("errata/api/release.json");
        String errataBuildsJsonString = TestResources.asString("errata/api/build_list.json");
        Errata errata = ObjectMapperProvider.json().readValue(errataJsonString, Errata.class);
        ErrataRelease release = ObjectMapperProvider.json().readValue(releaseJsonString, ErrataRelease.class);
        ErrataBuildList buildList = ObjectMapperProvider.json()
                .readValue(errataBuildsJsonString, ErrataBuildList.class);

        String umbErrataStatusChangeMsg = TestResources.asString("errata/umb/errata_status_change.json");

        when(errataClient.getErratum("139230")).thenReturn(errata);
        when(errataClient.getBuildsList("139230")).thenReturn(buildList);
        when(errataClient.getRelease("2227")).thenReturn(release);

        errataNotificationHandler.handle(umbErrataStatusChangeMsg);
    }

}
