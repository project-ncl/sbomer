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
package org.jboss.sbomer.cli.test.integ.feature.sbom.command;

import java.util.Set;

import org.jboss.sbomer.cli.test.integ.feature.sbom.AlternativePncService;
import org.jboss.sbomer.cli.test.integ.feature.sbom.command.CycloneDXPluginMavenGenerateCommandIT.CustomPncServiceProfile;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@TestProfile(CustomPncServiceProfile.class)
public class CycloneDXPluginMavenGenerateCommandIT {
    public static class CustomPncServiceProfile implements QuarkusTestProfile {

        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(AlternativePncService.class);
        }
    }

    @Test
    @Disabled("Figure out how to run a generation integration test")
    @DisplayName("Should run the cyclonedx generation")
    @Launch(value = { "-v", "generate", "--sbom-id", "123", "maven", "cyclonedx" })
    void shouldRunGeneration() throws Exception {
    }
}
