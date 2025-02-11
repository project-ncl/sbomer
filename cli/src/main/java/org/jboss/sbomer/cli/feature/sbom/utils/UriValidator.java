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
package org.jboss.sbomer.cli.feature.sbom.utils;

import org.dmfs.rfc3986.Uri;
import org.dmfs.rfc3986.encoding.Precoded;
import org.dmfs.rfc3986.uris.LazyUri;

/**
 * Validates URI according to RFC 3986
 */
public class UriValidator {
    private UriValidator() {
        throw new IllegalStateException("This is a utility class that should not be instantiated");
    }

    public static boolean isUriValid(String s) {
        Uri uri = new LazyUri(new Precoded(s));
        try {
            uri.fragment().isPresent();
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
