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
package org.jboss.sbomer.core.features.sbom.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class UrlUtils {

    public static final String UTF_8 = "utf-8";

    public static String urlencode(String value) {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to urlencode", e);
        }
    }

    public static String urldecode(String value) {
        try {
            return URLDecoder.decode(value, UTF_8);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to urldecode", e);
        }
    }
}
