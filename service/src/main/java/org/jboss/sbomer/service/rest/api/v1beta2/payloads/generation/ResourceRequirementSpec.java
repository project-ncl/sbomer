package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <p>
 * Configuration of CPU and memory for a given execution.
 * </p>
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResourceRequirementSpec(String cpu, String memory) {
}