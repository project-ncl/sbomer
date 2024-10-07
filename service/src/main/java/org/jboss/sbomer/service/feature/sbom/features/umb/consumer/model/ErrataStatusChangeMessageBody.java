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

import java.time.Instant;

import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataStatus;
import org.jboss.sbomer.service.feature.sbom.errata.dto.enums.ErrataType;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class ErrataStatusChangeMessageBody {
    @JsonProperty("errata_id")
    Long errataId;

    @JsonProperty("type")
    ErrataType type;

    @JsonProperty("errata_status")
    ErrataStatus status;

    ErrataStatus from;

    ErrataStatus to;

    @JsonProperty("fulladvisory")
    String fullAdvisory;

    String product;

    String release;

    String synopsis;

    Instant when;

    String who;

    @JsonProperty("content_types")
    String[] contentTypes;

}
