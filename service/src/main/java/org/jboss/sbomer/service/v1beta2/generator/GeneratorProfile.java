package org.jboss.sbomer.service.v1beta2.generator;

import java.util.List;

/**
 * Defines the profile for a type of generator, including all its supported versions.
 *
 * @param name name of the generator
 * @param description optional description of the generator
 * @param versions list of supported/available versions of a given generator
 */
public record GeneratorProfile(String name, String description, List<GeneratorVersionProfile> versions) {
}