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
package org.jboss.sbomer.service.feature.sbom.k8s.resources;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Labels used by Tekton resources within the SBOM feature.
 */
public class Labels {
    public static final String LABEL_PHASE = "sbomer.jboss.org/phase";
    public static final String LABEL_BUILD_ID = "sbomer.jboss.org/build-id";
    public static final String LABEL_SELECTOR = "app.kubernetes.io/part-of=sbomer,app.kubernetes.io/component=sbom,app.kubernetes.io/managed-by=sbom,sbomer.jboss.org/type=generation-request";

    public static Map<String, String> defaultLabelsToMap() {
        return Arrays.asList(LABEL_SELECTOR.split(","))
                .stream()
                .map(l -> l.split("="))
                .collect(Collectors.toMap(splitLabel -> splitLabel[0], splitLabel -> splitLabel[1]));
    }
}
