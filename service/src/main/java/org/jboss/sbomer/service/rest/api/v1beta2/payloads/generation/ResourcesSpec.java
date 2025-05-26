package org.jboss.sbomer.service.rest.api.v1beta2.payloads.generation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <p>
 * Configuration of resources for requests and limits for a given execution.
 * </p>
 *
 * <p>
 * Generator may or may not take this into account. This is a suggestion. Please check documentation of a given
 * generator for more information.
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ResourcesSpec(ResourceRequirementSpec requests, ResourceRequirementSpec limits) {
}