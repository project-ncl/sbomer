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
package org.jboss.sbomer.core.features.sbom.utils;

import java.util.regex.Pattern;

import io.quarkus.util.GlobUtil;

public class RhVersionPattern {

    private static final String RH_VERSION_SUFFIX = "?redhat-*";
    private static final Pattern RH_VERSION_SUFFIX_PATTERN = Pattern
            .compile(GlobUtil.toRegexPattern(RH_VERSION_SUFFIX));
    private static final String RH_VERSION_EXPR = "*redhat-*";
    private static final String RH_NPM_PURL_VERSION = ".*(?:%40redhat|@redhat).*";
    private static final Pattern RH_VERSION_PATTERN = Pattern.compile(GlobUtil.toRegexPattern(RH_VERSION_EXPR));
    private static final Pattern RH_NPM_PURL_PATTERN = Pattern.compile(RH_NPM_PURL_VERSION);

    public static boolean isRhVersion(String version) {
        return RH_VERSION_PATTERN.matcher(version).matches();
    }

    public static boolean isRhPurl(String purl) {
        return RH_NPM_PURL_PATTERN.matcher(purl).matches();
    }

    public static String ensureNoRhSuffix(String version) {
        return RH_VERSION_SUFFIX_PATTERN.matcher(version).replaceFirst("");
    }

}
