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
package org.jboss.sbomer.service.rest.mapper;

import org.jboss.sbomer.core.dto.v1beta1.V1Beta1SbomGenerationRequestRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1SbomRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1StatsRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1StatsRecord.V1Beta1StatsDeploymentRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1StatsRecord.V1Beta1StatsMessagingRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1StatsRecord.V1Beta1StatsResourceGenerationsRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1StatsRecord.V1Beta1StatsResourceManifestsRecord;
import org.jboss.sbomer.core.dto.v1beta1.V1Beta1StatsRecord.V1Beta1StatsResourceRecord;
import org.jboss.sbomer.service.feature.sbom.model.Stats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Deployment;
import org.jboss.sbomer.service.feature.sbom.model.Stats.GenerationRequestStats;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Messaging;
import org.jboss.sbomer.service.feature.sbom.model.Stats.Resources;
import org.jboss.sbomer.service.feature.sbom.model.Stats.SbomStats;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(config = MapperConfig.class)
public interface V1Beta1Mapper extends EntityMapper<V1Beta1SbomRecord, V1Beta1SbomGenerationRequestRecord> {

    V1Beta1StatsDeploymentRecord toRecord(Deployment deployment);

    V1Beta1StatsResourceGenerationsRecord toRecord(GenerationRequestStats stats);

    V1Beta1StatsResourceManifestsRecord toRecord(SbomStats stats);

    V1Beta1StatsMessagingRecord toRecord(Messaging messaging);

    @Mapping(target = "manifests", source = "resources.sboms")
    @Mapping(target = "generations", source = "resources.generationRequests")
    V1Beta1StatsResourceRecord toRecord(Resources resources);

    V1Beta1StatsRecord toRecord(Stats stats);
}
