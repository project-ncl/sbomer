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

import java.util.Collection;

import org.jboss.sbomer.core.dto.BaseSbomRecord;
import org.jboss.sbomer.core.dto.v1alpha3.SbomGenerationRequestRecord;
import org.jboss.sbomer.core.dto.v1alpha3.SbomRecord;
import org.jboss.sbomer.core.features.sbom.rest.Page;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.model.SbomGenerationRequest;
import org.mapstruct.Mapper;

// TODO: Currently uses v1alpha3 records
@Mapper(config = MapperConfig.class)
public interface V1Beta1Mapper {

    SbomRecord toSbomRecord(Sbom entity);

    BaseSbomRecord toSearchRecord(Sbom entity);

    SbomGenerationRequestRecord toSbomRequestRecord(SbomGenerationRequest entity);

    Collection<SbomGenerationRequestRecord> toSbomRequestRecords(Collection<SbomGenerationRequest> entities);

    Page<BaseSbomRecord> toSbomSearchRecordPage(Page<BaseSbomRecord> sboms);

    Page<SbomRecord> toSbomRecordPage(Page<Sbom> sboms);

    Page<SbomGenerationRequestRecord> toSbomRequestRecordPage(Page<SbomGenerationRequest> sbomRequests);

}
