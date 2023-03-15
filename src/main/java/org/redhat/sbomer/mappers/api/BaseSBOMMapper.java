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
package org.redhat.sbomer.mappers.api;

import org.redhat.sbomer.model.BaseSBOM;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        unmappedSourcePolicy = ReportingPolicy.WARN,
        unmappedTargetPolicy = ReportingPolicy.ERROR,
        implementationPackage = "org.redhat.sbomer.mappers",
        componentModel = "cdi")
public interface BaseSBOMMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "generationTime", ignore = true)
    @Mapping(target = "buildId", source = "buildId")
    @Mapping(target = "sbom", source = "bom")
    @BeanMapping(ignoreUnmappedSourceProperties = { "cycloneDxBom", "generationTime", "id" })
    BaseSBOM toEntity(org.redhat.sbomer.dto.BaseSBOM dtoEntity);

    @Mapping(target = "id", expression = "java( dbEntity.getId().toString() )")
    @Mapping(target = "buildId", source = "buildId")
    @Mapping(target = "generationTime", source = "generationTime")
    @Mapping(target = "bom", source = "sbom")
    @BeanMapping(ignoreUnmappedSourceProperties = { "persistent", "cycloneDxBom", "id" })
    org.redhat.sbomer.dto.BaseSBOM toDTO(BaseSBOM dbEntity);

}