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
package org.jboss.sbomer.core.features.sbom.utils.commandline.gradle;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.sbomer.core.features.sbom.Constants;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GradleCommandLineParser {

    private GradleCommandLineParser() {
        // This is a utility class
    }

    private static final String GRADLE_MAJOR_VERSION_REGEX_PREFIX = Constants.GRADLE_MAJOR_VERSION_COMMAND_PREFIX
            + "(\\d+)#(.*)";
    private static final Pattern GRADLE_MAJOR_VERSION_PREFIX_PATTERN = Pattern
            .compile(GRADLE_MAJOR_VERSION_REGEX_PREFIX);

    public static Optional<Integer> extractGradleMajorVersion(String buildCmdOptions) {
        if (buildCmdOptions == null || buildCmdOptions.trim().isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = GRADLE_MAJOR_VERSION_PREFIX_PATTERN.matcher(buildCmdOptions);
        if (matcher.find()) {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        }
        return Optional.empty();
    }

    public static Optional<String> extractGradleMainBuildCommand(String buildCmdOptions) {
        if (buildCmdOptions == null || buildCmdOptions.trim().isEmpty()) {
            return Optional.empty();
        }

        Matcher matcher = GRADLE_MAJOR_VERSION_PREFIX_PATTERN.matcher(buildCmdOptions);
        if (matcher.find()) {
            return Optional.of(matcher.group(2));
        }
        return Optional.of(buildCmdOptions);
    }

}
