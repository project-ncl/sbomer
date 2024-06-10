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
package org.jboss.sbomer.service.feature.sbom.service;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.core.pnc.PncService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class PncServiceProducer {

    @ConfigProperty(name = "sbomer.pnc.host")
    @Getter
    String apiUrl;

    @Produces
    @ApplicationScoped
    public PncService producePncService() {
        log.debug("Creating new PNC service bean...");
        return new PncService(apiUrl);
    }

    public void close(@Disposes PncService pncService) {
        log.debug("Closing the PNC service...");
        pncService.close();
    }
}
