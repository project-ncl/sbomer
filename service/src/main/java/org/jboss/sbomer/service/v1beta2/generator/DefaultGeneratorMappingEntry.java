package org.jboss.sbomer.service.v1beta2.generator;

import java.util.List;

/**
 * Represents a single mapping from a content type to a list of preferred generators.
 *
 * @param type the type of the content to manifest, for example {@code CONTAINER_IMAGE} or {@code MAVEN_PROJECT}
 * @param generators a list of generators that support given type. First in the list is the preferred generator.
 */
public record DefaultGeneratorMappingEntry(String targetType, List<String> generators) {
}