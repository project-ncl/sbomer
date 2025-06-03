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
package org.jboss.sbomer.core.dto.v1beta1;

import java.time.Instant;
import java.util.StringJoiner;

public record V1Beta1RequestManifestRecord(String id, String identifier, String rootPurl, Instant creationTime,
        Integer configIndex, String statusMessage, V1Beta1GenerationRecord generation) {

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "V1Beta1RequestManifestRecord[", "]");

        if (id != null)
            joiner.add("id=" + id);
        if (identifier != null)
            joiner.add("identifier=" + identifier);
        if (rootPurl != null)
            joiner.add("rootPurl=" + rootPurl);
        if (creationTime != null)
            joiner.add("creationTime=" + creationTime);
        if (configIndex != null)
            joiner.add("configIndex=" + configIndex);
        if (statusMessage != null)
            joiner.add("statusMessage=" + statusMessage);
        if (generation != null)
            joiner.add("generation=" + generation);

        return joiner.toString();
    }

}
