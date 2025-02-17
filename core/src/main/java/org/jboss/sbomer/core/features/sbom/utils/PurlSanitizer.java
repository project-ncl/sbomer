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

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.TreeMap;

@Slf4j
public class PurlSanitizer {

    private static final String NAME_VERSION_QKEY_QVALUE = "[^a-zA-Z0-9.+\\-_]";
    private static final String TYPE_INVALID_CHARS = "[^a-zA-Z0-9.+-]";

    private PurlSanitizer() {
        throw new IllegalStateException("This is a utility class that should not be instantiated");
    }

    /**
     * Sanitize a given PURL string by replacing invalid characters in each component.
     *
     * @param purl the original PURL string
     * @return sanitized PURL string
     */
    public static String sanitizePurl(String purl) {
        if (purl == null || purl.isEmpty()) {
            throw new IllegalArgumentException("PURL cannot be null or empty");
        }

        log.debug("Sanitizing purl {}...", purl);

        // Attempt to parse the PURL using PackageURL
        try {
            PackageURL parsedPurl = new PackageURL(purl);
            return parsedPurl.canonicalize();
        } catch (MalformedPackageURLException e) {
            // If parsing fails, proceed to manual sanitization
            log.error("Malformed PURL detected, attempting to sanitize: {}", purl);
        }

        // Manually parse and sanitize the PURL components
        try {
            // Split PURL into components
            String[] purlParts = purl.split("\\?", 2);
            String mainPart = purlParts[0];
            String qualifiersPart = purlParts.length > 1 ? purlParts[1] : null;

            // Extract scheme
            if (mainPart.startsWith("pkg:")) {
                mainPart = mainPart.substring(4);
            }

            // Extract subpath if present
            String[] mainPartSplit = mainPart.split("#", 2);
            mainPart = mainPartSplit[0];
            String subpath = mainPartSplit.length > 1 ? mainPartSplit[1] : null;

            // Extract type, namespace, name, version
            String type;
            String namespace = null;
            String name;
            String version = null;

            int firstSlash = mainPart.indexOf('/');
            int atSign = mainPart.indexOf('@');

            if (firstSlash >= 0) {
                type = sanitizeType(mainPart.substring(0, firstSlash));
                String remainder = mainPart.substring(firstSlash + 1);

                if (atSign >= 0) {
                    // Version is present
                    atSign = remainder.indexOf('@');
                    if (atSign >= 0) {
                        version = sanitizeVersion(remainder.substring(atSign + 1));
                        remainder = remainder.substring(0, atSign);
                    }
                }

                // Namespace and name
                int lastSlash = remainder.lastIndexOf('/');
                if (lastSlash >= 0) {
                    namespace = sanitizeNamespace(remainder.substring(0, lastSlash));
                    name = sanitizeName(remainder.substring(lastSlash + 1));
                } else {
                    name = sanitizeName(remainder);
                }
            } else {
                throw new MalformedPackageURLException("Invalid PURL format, missing type and name.");
            }

            // Sanitize qualifiers
            TreeMap<String, String> qualifiers = parseQualifiers(qualifiersPart);
            if (qualifiers != null) {
                qualifiers = sanitizeQualifiers(qualifiers);
            }

            // Sanitize subpath
            subpath = sanitizeSubpath(subpath);

            // Reconstruct the sanitized PURL
            PackageURL sanitizedPurl = new PackageURL(type, namespace, name, version, qualifiers, subpath);
            return sanitizedPurl.canonicalize();

        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to sanitize PURL: " + purl, ex);
        }
    }

    public static String sanitizeType(String type) {
        return type.replaceAll(TYPE_INVALID_CHARS, "-").toLowerCase();
    }

    public static String sanitizeNamespace(String namespace) {
        return sanitizeSubpath(namespace);
    }

    public static String sanitizeName(String name) {
        return name.replaceAll(NAME_VERSION_QKEY_QVALUE, "-");
    }

    public static String sanitizeVersion(String version) {
        if (version == null)
            return null;
        return version.replaceAll(NAME_VERSION_QKEY_QVALUE, "-");
    }

    public static String sanitizeSubpath(String subpath) {
        if (subpath == null)
            return null;
        String[] segments = subpath.split("/");
        for (int i = 0; i < segments.length; i++) {
            segments[i] = segments[i].replaceAll(NAME_VERSION_QKEY_QVALUE, "-");
        }
        return String.join("/", segments);
    }

    public static TreeMap<String, String> sanitizeQualifiers(TreeMap<String, String> qualifiers) { // NOSONAR: This
                                                                                                   // should be Map, but
                                                                                                   // PackageURL
                                                                                                   // requires TreeMap
        if (qualifiers == null) {
            return null; // NOSONAR: Should return an empty map, but PackageURL expects null
        }
        TreeMap<String, String> sanitized = new TreeMap<>();
        for (Map.Entry<String, String> entry : qualifiers.entrySet()) {
            String key = entry.getKey().replaceAll(NAME_VERSION_QKEY_QVALUE, "");
            String value = entry.getValue().replaceAll(NAME_VERSION_QKEY_QVALUE, "-");
            sanitized.put(key, value);
        }
        return sanitized;
    }

    private static TreeMap<String, String> parseQualifiers(String qualifiersPart) {
        if (qualifiersPart == null || qualifiersPart.isEmpty()) {
            return null; // NOSONAR: Should return an empty map, but PackageURL expects null
        }
        TreeMap<String, String> qualifiers = new TreeMap<>();
        String[] pairs = qualifiersPart.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                qualifiers.put(keyValue[0], keyValue[1]);
            }
        }
        return qualifiers;
    }

}
