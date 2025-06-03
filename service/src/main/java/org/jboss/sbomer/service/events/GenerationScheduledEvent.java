package org.jboss.sbomer.service.events;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.service.feature.sbom.model.v1beta2.dto.GenerationRecord;
import org.jboss.sbomer.service.v1beta2.controller.request.Request;
import org.jboss.sbomer.service.v1beta2.controller.request.RequestType;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.slf4j.Slf4j;

/**
 * Event fired after a Generation is scheduled.
 */
@Slf4j
public record GenerationScheduledEvent(GenerationRecord generation) {

    public boolean isOfRequestType(RequestType type) {
        if (this.generation == null) {
            log.warn("Generation was not populated");
            return false;
        }

        if (this.generation.request() == null) {
            log.warn("Request was not provided");
            return false;
        }

        Request request = null;

        // Parse request
        try {
            request = ObjectMapperProvider.json().treeToValue(generation.request(), Request.class);
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Unable to parse provided resource configuration", e);
        }

        if (request.target().type() == type) {
            return true;
        }

        return false;
    }
}
