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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;
import com.github.packageurl.PackageURLBuilder;

public class UrlUtils {

    private UrlUtils() {
        // This is a utility class
    }

    public static String urlencode(String value) {
        return URLEncoder.encode(value, UTF_8);
    }

    public static String urldecode(String value) {
        return URLDecoder.decode(value, UTF_8);
    }

    /**
     * Removes from the provided purl the qualifiers that are present (if any) in the removeList. If the purl does not
     * contain any qualifier that needs to be removed, the original purl is returned, otherwise a new purl is built and
     * returned.
     *
     * @param purl the purl from which the qualifiers should be removed
     * @param removeList the list of qualifiers which should be removed from the purl
     * @return the purl with the qualifiers removed
     */
    public static String removeQualifiersFromPurl(String purl, List<String> removeList) {
        try {
            PackageURL packageURL = new PackageURL(purl);
            Map<String, String> qualifiers = packageURL.getQualifiers();
            if (qualifiers == null || qualifiers.isEmpty() || removeList == null || removeList.isEmpty()) {
                // Nothing to remove!
                return purl;
            }

            PackageURLBuilder builder = packageURL.toBuilder();
            removeList.forEach(builder::withoutQualifier);
            return builder.build().toString();
        } catch (MalformedPackageURLException e) {
            // Just return the originally provided purl
            return purl;
        }
    }
}
