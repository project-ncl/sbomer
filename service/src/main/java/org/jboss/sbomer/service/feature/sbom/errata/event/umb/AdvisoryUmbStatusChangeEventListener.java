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
import org.jboss.sbomer.service.feature.sbom.features.umb.consumer.ErrataNotificationHandler;
import org.jboss.sbomer.service.feature.sbom.service.RequestEventRepository;
import org.slf4j.MDC;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class AdvisoryUmbStatusChangeEventListener {

    @Inject
    RequestEventRepository requestEventRepository;

    @Inject
    ErrataNotificationHandler errataNotificationHandler;

    public void onAdvisoryStatusUpdate(@ObservesAsync MdcEventWrapper wrapper) {

        Map<String, String> mdcContext = wrapper.getMdcContext();
        if (mdcContext != null) {
            MDC.setContextMap(mdcContext);
        } else {
            MDC.clear();
        }

        AdvisoryUmbStatusChangeEvent event = (AdvisoryUmbStatusChangeEvent) wrapper.getPayload();

        try {
            errataNotificationHandler.handle(event.getRequestEventId());
        } catch (IOException e) {
            log.error("Unable to deserialize Errata message, this is unexpected", e);
            requestEventRepository.updateWithGenericFailure(event.getRequestEventId());
        } catch (ApplicationException exc) {
            log.error(
                    "Received error while handing errata request '{}': {}",
                    event.getRequestEventId(),
                    exc.getMessage());
            requestEventRepository.updateWithGenericFailure(event.getRequestEventId());
        } catch (RuntimeException exc) {
            log.error("Received error while handing request '{}'", event.getRequestEventId(), exc);
            requestEventRepository.updateWithGenericFailure(event.getRequestEventId());
        } finally {
            MDC.clear();
        }
    }

}
