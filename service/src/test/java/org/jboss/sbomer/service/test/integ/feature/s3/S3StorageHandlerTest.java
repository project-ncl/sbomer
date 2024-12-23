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
package org.jboss.sbomer.service.test.integ.feature.s3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.s3.S3StorageHandler;
import org.jboss.sbomer.service.feature.sbom.config.GenerationRequestControllerConfig;
import org.jboss.sbomer.service.feature.sbom.k8s.model.GenerationRequest;
import org.jboss.sbomer.service.test.utils.umb.TestUmbProfile;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.mockito.InjectSpy;
import jakarta.inject.Inject;

@QuarkusTest
@TestProfile(TestUmbProfile.class)
public class S3StorageHandlerTest {

    @Inject
    S3StorageHandler storageHandler;

    @InjectSpy
    FeatureFlags featureFlags;

    @InjectMock
    GenerationRequestControllerConfig controllerConfig;

    @Test
    void testMissingConfiguration() {
        when(featureFlags.s3Storage()).thenReturn(true);
        when(controllerConfig.sbomDir()).thenReturn("/tmp/blah");

        ApplicationException ex = assertThrows(ApplicationException.class, () -> {
            storageHandler.storeFiles(mock(GenerationRequest.class));
        });

        assertEquals(
                "S3 client cannot be instantiated, following env variables are missing: [AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, BUCKET_REGION, BUCKET_NAME]",
                ex.getMessage());
    }
}
