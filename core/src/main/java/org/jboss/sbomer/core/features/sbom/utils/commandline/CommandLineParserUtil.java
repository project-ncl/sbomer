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
package org.jboss.sbomer.core.features.sbom.utils.commandline;

import java.util.Map;
import java.util.Optional;

import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.core.features.sbom.Constants;
import org.jboss.sbomer.core.features.sbom.utils.EnvironmentAttributesUtils;
import org.jboss.sbomer.core.features.sbom.utils.commandline.maven.MavenCommandLineParser;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CommandLineParserUtil {

    private CommandLineParserUtil() {
        // This is a utility class
    }

    public static String getLaunderedCommandScript(Build build) {
        String buildCmdOptions = "";
        if (org.jboss.pnc.enums.BuildType.MVN.equals(build.getBuildConfigRevision().getBuildType())) {
            buildCmdOptions = "mvn";
            try {
                MavenCommandLineParser lineParser = MavenCommandLineParser.build()
                        .launder(build.getBuildConfigRevision().getBuildScript());
                buildCmdOptions = lineParser.getRebuiltMvnCommandScript();
            } catch (IllegalArgumentException exc) {
                log.error("Could not launder the provided build command script! Using the default build command", exc);
            }
        } else if (org.jboss.pnc.enums.BuildType.GRADLE.equals(build.getBuildConfigRevision().getBuildType())) {
            Optional<String> gradleMajorVersion = EnvironmentAttributesUtils
                    .getGradleSDKManCompliantMajorVersion(build.getEnvironment().getAttributes());
            if (gradleMajorVersion.isPresent()) {
                buildCmdOptions += (Constants.GRADLE_MAJOR_VERSION_COMMAND_PREFIX + gradleMajorVersion.get().trim()
                        + "#");
            }

            buildCmdOptions += "gradle";
            // It looks like we need to override the final version as it might not be picked up in the CycloneDX
            // generation, which would be overridden by the gradle.properties. The BREW_BUILD_VERSION attribute contains
            // the version we need.
            Optional<String> versionOverride = getVersionFromBuildAttributes(build);
            if (versionOverride.isPresent()) {
                buildCmdOptions += " -Pversion=" + versionOverride.get();
            }
        } else if (org.jboss.pnc.enums.BuildType.NPM.equals(build.getBuildConfigRevision().getBuildType())) {
            Optional<String> versionOverride = getVersionFromBuildAttributes(build);
            if (versionOverride.isPresent()) {
                buildCmdOptions += "npm version " + versionOverride.get();
            }
        }
        return buildCmdOptions;
    }

    private static Optional<String> getVersionFromBuildAttributes(Build build) {
        Map<String, String> attributes = build.getAttributes();
        return attributes != null && !attributes.isEmpty()
                && attributes.containsKey(Constants.BUILD_ATTRIBUTES_BREW_BUILD_VERSION)
                        ? Optional.of(attributes.get(Constants.BUILD_ATTRIBUTES_BREW_BUILD_VERSION))
                        : Optional.empty();
    }
}
