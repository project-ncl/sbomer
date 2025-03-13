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
package org.jboss.sbomer.cli.test.integ.feature.sbom.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.sbomer.cli.test.utils.PncWireMock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

/**
 * General tests that apply to all generation commands.
 */
@QuarkusMainTest
@WithTestResource(PncWireMock.class)
class BaseGenerateCommandIT {
    @Test
    @DisplayName("Should fail in case of build not found in PNC")
    void shouldFailForMissingBuild(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("-v", "sbom", "generate", "--build-id", "NOTEXISTING", "maven-cyclonedx");
        assertEquals(33, result.exitCode());
        assertTrue(result.getErrorOutput().contains("Could not fetch the PNC build with id 'NOTEXISTING'"));
    }

    @Test
    @DisplayName("Should fail in case the PNC build is still in progress")
    void shouldFailForBuildInProgress(QuarkusMainLauncher launcher) {
        LaunchResult result = launcher.launch("-v", "sbom", "generate", "--build-id", "IN_PROGRESS", "maven-cyclonedx");
        assertEquals(31, result.exitCode());
        assertTrue(
                result.getErrorOutput()
                        .contains(
                                "Build 'IN_PROGRESS' is not valid! Build cannot be temporary and progress needs to be 'FINISHED' with status 'SUCCESS' or 'NO_REBUILD_REQUIRED'. Currently: temporary: false, progress: 'IN_PROGRESS', status: 'BUILDING'"));
    }

}
