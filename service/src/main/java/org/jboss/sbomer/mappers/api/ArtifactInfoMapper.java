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
package org.jboss.sbomer.mappers.api;

import org.jboss.sbomer.dto.ArtifactInfo;
import org.jboss.pnc.dto.Artifact;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedSourcePolicy = ReportingPolicy.WARN,
        unmappedTargetPolicy = ReportingPolicy.WARN,
        implementationPackage = "org.jboss.sbomer.mappers",
        componentModel = "cdi")
public interface ArtifactInfoMapper {

    @Mapping(target = "identifier", source = "identifier")
    @Mapping(target = "purl", source = "purl")
    @Mapping(target = "md5", source = "md5")
    @Mapping(target = "sha1", source = "sha1")
    @Mapping(target = "sha256", source = "sha256")
    @Mapping(target = "buildId", expression = "java( artifact.getBuild().getId().toString() )")
    @Mapping(target = "publicUrl", source = "publicUrl")
    @Mapping(target = "originUrl", source = "originUrl")
    @Mapping(target = "scmUrl", expression = "java( artifact.getBuild().getScmUrl() )")
    @Mapping(target = "scmRevision", expression = "java( artifact.getBuild().getScmRevision() )")
    @Mapping(target = "scmTag", expression = "java( artifact.getBuild().getScmTag() )")
    @Mapping(target = "scmExternalUrl", expression = "java( artifact.getBuild().getScmRepository().getExternalUrl() )")
    @Mapping(
            target = "environmentImage",
            expression = "java( artifact.getBuild().getEnvironment().getSystemImageRepositoryUrl() + \"/\" + artifact.getBuild().getEnvironment().getSystemImageId() )")
    @Mapping(target = "buildSystem", ignore = true)
    @BeanMapping(
            ignoreUnmappedSourceProperties = { "artifactQuality", "buildCategory", "creationTime", "deployPath",
                    "deployUrl", "filename", "id", "importDate", "modificationTime", "qualityLevelReason", "size",
                    "build", "creationUser", "modificationUser", "targetRepository" })
    ArtifactInfo toArtifactInfo(Artifact artifact);

}
