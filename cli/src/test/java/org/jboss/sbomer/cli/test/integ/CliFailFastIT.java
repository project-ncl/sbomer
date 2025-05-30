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
package org.jboss.sbomer.cli.test.integ;

import java.nio.file.Path;
import java.util.Set;

import org.jboss.sbomer.cli.test.integ.CliFailFastIT.CustomPncServiceProfile;
import org.jboss.sbomer.cli.test.utils.FailedDefaultProcessCommand;
import org.jboss.sbomer.cli.test.utils.MavenCycloneDxGenerateCommandMockAlternative;
import org.jboss.sbomer.cli.test.utils.PncWireMock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
@WithTestResource(PncWireMock.class)
@TestProfile(CustomPncServiceProfile.class)
class CliFailFastIT {
    public static class CustomPncServiceProfile implements QuarkusTestProfile {
        @Override
        public Set<Class<?>> getEnabledAlternatives() {
            return Set.of(MavenCycloneDxGenerateCommandMockAlternative.class, FailedDefaultProcessCommand.class);
        }
    }

    @Test
    @DisplayName("Should fail after default processor fails")
    void testShouldNotRunNextCommandIfPreviousFails(QuarkusMainLauncher launcher, @TempDir Path tempDir) {

        LaunchResult result = launcher.launch(
                "-v",
                "sbom",
                "generate",
                "--workdir",
                tempDir.resolve("project").toAbsolutePath().toString(),
                "--output",
                tempDir.resolve("bom.json").toAbsolutePath().toString(),
                "--build-id",
                "QUARKUS",
                "maven-cyclonedx",
                "process",
                "default",
                "redhat-product",
                "--productName",
                "PRNAME",
                "--productVersion",
                "PRVERSION",
                "--productVariant",
                "PRVARIANT");

        Assertions.assertEquals(333, result.exitCode());
    }
}
