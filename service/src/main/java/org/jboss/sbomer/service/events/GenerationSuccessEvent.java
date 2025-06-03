package org.jboss.sbomer.service.events;

import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.GenerationRecord;

/**
 * Event fired after successful finish of a particular generation.
 */
public record GenerationSuccessEvent(GenerationRecord generation) {

}
