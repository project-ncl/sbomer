package org.jboss.sbomer.service.events;

import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.GenerationRecord;

public record GenerationRequestEvent(GenerationRecord generation) {

}
