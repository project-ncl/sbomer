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
package org.jboss.sbomer.service.feature.sbom.errata;

import java.util.HashMap;
import java.util.Map;

import javax.validation.constraints.Positive;

import jakarta.validation.constraints.Max;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import lombok.Builder;
import lombok.Data;

@Data
@Builder(setterPrefix = "with")
public class ErrataQueryParameters {

    @Builder.Default
    @QueryParam("filter")
    Map<String, String> filters = new HashMap<>();

    @QueryParam("page[number]")
    @DefaultValue(value = "1")
    @Positive
    @Builder.Default
    int pageNumber = 1;

    @QueryParam("page[size]")
    @DefaultValue(value = "100")
    @Positive
    @Max(value = 200)
    @Builder.Default
    int pageSize = 200;

}
