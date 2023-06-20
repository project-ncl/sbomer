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
package org.jboss.sbomer.core.features.sbomer.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbomer.config.runtime.ProcessorConfig;
import org.jboss.sbomer.core.features.sbomer.enums.ProcessorType;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Deserializer to help reading processors configuration from the SBOMer configuration file.
 *
 * @author Marek Goldmann
 */
public class ProcessorsDeserializer extends StdDeserializer<Map<ProcessorType, ProcessorConfig>> {

    public ProcessorsDeserializer() {
        this(null);
    }

    public ProcessorsDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Map<ProcessorType, ProcessorConfig> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException, JacksonException {

        JsonNode node = p.getCodec().readTree(p);

        if (!node.isObject()) {
            throw new ApplicationException("Invalid format of processors, a map expected");
        }

        Map<ProcessorType, ProcessorConfig> processors = new HashMap<>();
        var om = new ObjectMapper();

        ((ObjectNode) node).fields().forEachRemaining(entry -> {
            if (!entry.getValue().isObject()) {
                throw new ApplicationException(
                        "Invalid format of processor, '{}' has invalid processor configuration",
                        entry.getKey());
            }

            Optional<ProcessorType> processorImplementation = ProcessorType.get(entry.getKey());

            if (processorImplementation.isEmpty()) {
                throw new ApplicationException("Processor implementation: '{}' is not valid", entry.getKey());
            }

            ObjectNode processor = entry.getValue().deepCopy();
            processor.put("@type", entry.getKey());

            ProcessorConfig config = om.convertValue(processor, ProcessorConfig.class);

            processors.put(processorImplementation.get(), config);
        });

        return processors;
    }
}
