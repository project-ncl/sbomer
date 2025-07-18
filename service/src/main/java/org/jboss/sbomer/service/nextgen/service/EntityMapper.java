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
package org.jboss.sbomer.service.nextgen.service;

import java.util.List;

import org.jboss.sbomer.service.nextgen.core.dto.model.EventRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.EventStatusRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationStatusRecord;
import org.jboss.sbomer.service.nextgen.core.dto.model.ManifestRecord;
import org.jboss.sbomer.service.nextgen.service.model.Event;
import org.jboss.sbomer.service.nextgen.service.model.EventStatusHistory;
import org.jboss.sbomer.service.nextgen.service.model.Generation;
import org.jboss.sbomer.service.nextgen.service.model.GenerationStatusHistory;
import org.jboss.sbomer.service.nextgen.service.model.Manifest;
import org.jboss.sbomer.service.rest.mapper.MapperConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import jakarta.enterprise.context.ApplicationScoped;

@Mapper(config = MapperConfig.class)
@ApplicationScoped
public interface EntityMapper {
    @BeanMapping(unmappedSourcePolicy = ReportingPolicy.IGNORE)
    String toIdentifier(Event value);

    @BeanMapping(unmappedSourcePolicy = ReportingPolicy.IGNORE)
    String toIdentifier(Generation value);

    @BeanMapping(ignoreUnmappedSourceProperties = "persistent")
    EventRecord toRecord(Event event);

    @BeanMapping(ignoreUnmappedSourceProperties = "persistent")
    List<EventRecord> toEventRecords(List<Event> events);

    @BeanMapping(ignoreUnmappedSourceProperties = "persistent")
    List<ManifestRecord> toManifestRecords(List<Manifest> manifests);

    @BeanMapping(ignoreUnmappedSourceProperties = "persistent")
    GenerationRecord toRecord(Generation generation);

    @BeanMapping(ignoreUnmappedSourceProperties = "persistent")
    ManifestRecord toRecord(Manifest manifest);

    @BeanMapping(ignoreUnmappedSourceProperties = "persistent")
    List<GenerationRecord> toGenerationRecords(List<Generation> generations);

    @BeanMapping(ignoreUnmappedSourceProperties = "persistent")
    GenerationStatusRecord toRecord(GenerationStatusHistory status);

    @BeanMapping(ignoreUnmappedSourceProperties = "persistent")
    List<GenerationStatusRecord> toGenerationStatusRecords(List<GenerationStatusHistory> statuses);

    @BeanMapping(ignoreUnmappedSourceProperties = "persistent")
    EventStatusRecord toRecord(EventStatusHistory status);

    @BeanMapping(ignoreUnmappedSourceProperties = "persistent")
    List<EventStatusRecord> toEventStatusRecords(List<EventStatusHistory> statuses);
}
