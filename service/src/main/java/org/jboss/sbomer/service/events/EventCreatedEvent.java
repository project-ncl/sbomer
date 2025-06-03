package org.jboss.sbomer.service.events;

import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.EventRecord;

/**
 * An event fired after a particular {@link EventRecord} has been created.
 */
public record EventCreatedEvent(EventRecord event) {

}
