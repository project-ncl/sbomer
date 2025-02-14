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

import java.util.regex.Pattern;

public class CommandLineInspectorUtil {

    /*
     * Parses the build script to find the main build commands to understand the nature of the project. It's a best
     * attempt because we don't have the source code here (would be best to search for e.g. build.gradle | pom.xml |
     * package.json + yarn.lock | package.json + package-lock, build.sbt files)
     */
    private CommandLineInspectorUtil() {
    }

    private static final String MVN_BUILD_REGEXP = "\\s*.*(\\.?\\/)?mvn(w)?\\s+(\\s*.*)*(deploy)\\b\\s*.*";
    private static final String GRADLE_BUILD_REGEXP = "\\s*.*(\\.?\\/)?gradle(w)?\\s+(\\s*.*)*(build|assemble|publish)\\b\\s*.*";
    private static final String NPM_BUILD_REGEXP = "(\\s*.*npm\\s+(install|run|exec|build)\\s*.*)";
    private static final String YARN_BUILD_REGEXP = "(\\s*.*yarn\\s+(install|run|exec|build)\\s*.*)";

    private static final Pattern MVN_PATTERN = Pattern.compile(MVN_BUILD_REGEXP);
    private static final Pattern GRADLE_PATTERN = Pattern.compile(GRADLE_BUILD_REGEXP);
    private static final Pattern NPM_PATTERN = Pattern.compile(NPM_BUILD_REGEXP);
    private static final Pattern YARN_PATTERN = Pattern.compile(YARN_BUILD_REGEXP);

    public static boolean hasMavenEvidence(String buildScript) {
        return MVN_PATTERN.matcher(buildScript.replace("\n", "")).matches();
    }

    public static boolean hasGradleEvidence(String buildScript) {
        return GRADLE_PATTERN.matcher(buildScript.replace("\n", "")).matches();
    }

    public static boolean hasNpmEvidence(String buildScript) {
        return NPM_PATTERN.matcher(buildScript.replace("\n", "")).matches();
    }

    public static boolean hasYarnEvidence(String buildScript) {
        return YARN_PATTERN.matcher(buildScript.replace("\n", "")).matches();
    }

}
