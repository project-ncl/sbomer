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

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.jboss.sbomer.core.features.sbom.utils.CustomInstantDeserializer;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.model.ErrataStatusChangeMessageBody;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class ErrataMessageHelper {

    static ObjectMapper customJsonObjectMapper = ObjectMapperProvider.json()
            .registerModule(new SimpleModule().addDeserializer(Instant.class, new CustomInstantDeserializer()));

    public static String decode(byte[] encodedJson) {
        return new String(encodedJson, StandardCharsets.US_ASCII);
    }

    public static ErrataStatusChangeMessageBody fromStatusChangeMessage(String json) throws JsonProcessingException {
        return customJsonObjectMapper.readValue(json, ErrataStatusChangeMessageBody.class);
    }

}
