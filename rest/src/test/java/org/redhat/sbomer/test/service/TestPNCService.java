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
package org.redhat.sbomer.test.service;

import javax.inject.Inject;

import org.jboss.pnc.client.Configuration;
import org.junit.jupiter.api.Test;
import org.redhat.sbomer.test.mock.PncServiceMock;

import io.quarkus.test.junit.QuarkusTest;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
public class TestPNCService {

    @Inject
    PncServiceMock service;

    @Test
    void testConfigurationWithCustomCacheUrl() {
        Configuration config = service.getClientConfiguration();
        assertEquals("localhost/pnc/orch", config.getHost());
        assertEquals("http", config.getProtocol());
    }
}
