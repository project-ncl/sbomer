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
package org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model;

import org.jboss.pnc.api.enums.BuildStatus;
import org.jboss.pnc.api.enums.BuildType;
import org.jboss.pnc.api.enums.ProgressStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PncBuildNotificationMessageBody {
    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Build {
        String id;
        BuildConfigRevision buildConfigRevision;
        ProgressStatus progress;
        BuildStatus status;
        boolean temporaryBuild;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BuildConfigRevision {
        String id;
        BuildType buildType;
    }

    Build build;
}
