package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConfigSpec(GeneratorOptionsSpec generator, // Optional, if not provided, SBOMer resolves default
        String format, ResourcesSpec resources) {
}