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
package org.redhat.sbomer.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString
@EqualsAndHashCode
@JsonDeserialize(builder = ArtifactInfo.Builder.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactInfo {

    private final String identifier;
    private final String purl;
    private final String md5;
    private final String sha1;
    private final String sha256;
    private final String buildId;
    private final String publicUrl;
    private final String originUrl;
    private final String scmUrl;
    private final String scmRevision;
    private final String scmTag;
    private final String scmExternalUrl;
    private final String environmentImage;
    private String buildSystem = "PNC";

    @lombok.Builder(builderClassName = "Builder", toBuilder = true)
    private ArtifactInfo(
            String identifier,
            String purl,
            String md5,
            String sha1,
            String sha256,
            String buildId,
            String publicUrl,
            String originUrl,
            String scmUrl,
            String scmRevision,
            String scmTag,
            String scmExternalUrl,
            String environmentImage,
            String buildSystem) {
        this.identifier = identifier;
        this.purl = purl;
        this.md5 = md5;
        this.sha1 = sha1;
        this.sha256 = sha256;
        this.buildId = buildId;
        this.publicUrl = publicUrl;
        this.originUrl = originUrl;
        this.scmUrl = scmUrl;
        this.scmRevision = scmRevision;
        this.scmTag = scmTag;
        this.scmExternalUrl = scmExternalUrl;
        this.environmentImage = environmentImage;
        this.buildSystem = buildSystem;
    }

    @JsonPOJOBuilder(withPrefix = "")
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Builder {
    }

}
