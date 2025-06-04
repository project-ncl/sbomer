package org.jboss.sbomer.service.events;

import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.EventRecord;

/**
 * Event sent after a request for resolution is received.
 */
public record ResolveRequestEvent(EventRecord event) {

}
