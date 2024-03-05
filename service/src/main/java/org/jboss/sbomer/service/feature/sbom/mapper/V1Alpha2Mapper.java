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
package org.jboss.sbomer.service.feature.sbom.mapper;

import java.util.List;

import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface V1Alpha2Mapper {

    @Mapping(source = "identifier", target = "buildId")
    org.jboss.sbomer.core.dto.v1alpha2.SbomRecord toSbomRecord(Sbom entity);

    @Mapping(source = "identifier", target = "buildId")
    // @Mapping(target = "reason", expression = "java(reason == null ? '' : reason)")
    org.jboss.sbomer.core.dto.v1alpha2.SbomGenerationRequestRecord toSbomRequestRecord(SbomGenerationRequest entity);

    Page<org.jboss.sbomer.core.dto.v1alpha2.SbomRecord> toSbomRecordPage(Page<Sbom> sboms);

    Page<org.jboss.sbomer.core.dto.v1alpha2.SbomGenerationRequestRecord> toSbomRequestRecordPage(
            Page<SbomGenerationRequest> sbomRequests);

    List<org.jboss.sbomer.core.dto.v1alpha2.SbomRecord> toSbomRecordList(List<Sbom> sboms);

    @Mapping(source = "identifier", target = "buildId")
    org.jboss.sbomer.core.dto.v1alpha2.SbomRecord toSbomRecord(
            org.jboss.sbomer.core.dto.v1alpha3.BaseSbomRecord record);

    @Mapping(source = "identifier", target = "buildId")
    // @Mapping(target = "reason", expression = "java(reason == null ? '' : reason)")
    org.jboss.sbomer.core.dto.v1alpha2.SbomGenerationRequestRecord toSbomRequestRecord(
            org.jboss.sbomer.core.dto.v1alpha3.BaseSbomGenerationRequestRecord record);

    List<org.jboss.sbomer.core.dto.v1alpha2.SbomRecord> toV2SbomRecordList(
            List<org.jboss.sbomer.core.dto.v1alpha3.BaseSbomRecord> sboms);

    Page<org.jboss.sbomer.core.dto.v1alpha2.SbomRecord> toV2SbomRecordPage(
            Page<org.jboss.sbomer.core.dto.v1alpha3.BaseSbomRecord> sboms);

}
