/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.sbomer.service.feature.sbom.errata.event.umb;

import java.io.IOException;
import java.util.Map;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.service.feature.sbom.errata.event.util.MdcEventWrapper;
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.PncNotificationHandler;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;
import org.slf4j.MDC;

import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class PncBuildUmbStatusChangeEventListener {

    @Inject
    RequestEventRepository requestEventRepository;

    @Inject
    PncNotificationHandler pncNotificationHandler;

    public void onPncBuildStatusUpdate(@ObservesAsync MdcEventWrapper wrapper) {
        Object payload = wrapper.getPayload();
        if (!(payload instanceof PncBuildUmbStatusChangeEvent event)) {
            return;
        }

        Map<String, String> mdcContext = wrapper.getMdcContext();
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        } else {
            MDC.clear();
        }

        try {
            pncNotificationHandler.handle(event.getRequestEventId());
        } catch (JsonProcessingException e) {
            log.error("Unable to deserialize PNC message for request '{}'.", event.getRequestEventId(), e);
            requestEventRepository
                    .updateWithFailure(event.getRequestEventId(), "Unable to deserialize the PNC UMB message");
        } catch (ApplicationException e) {
            log.error("Application error while handling request '{}': {}", event.getRequestEventId(), e.getMessage());
            requestEventRepository.updateWithFailure(
                    event.getRequestEventId(),
                    "Application error occurred while processing the PNC UMB message: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("Unexpected error while handling request '{}'", event.getRequestEventId(), e);
            requestEventRepository
                    .updateWithFailure(event.getRequestEventId(), "Unexpected error occurred: " + e.getMessage());
        } finally {
            MDC.clear();
        }
    }

}
