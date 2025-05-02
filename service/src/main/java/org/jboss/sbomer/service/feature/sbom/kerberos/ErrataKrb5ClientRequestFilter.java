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
package org.jboss.sbomer.service.feature.sbom.kerberos;

import java.io.IOException;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ErrataKrb5ClientRequestFilter implements ClientRequestFilter {

    private static final String AUTHORIZATION = "Authorization";
    private static final String NEGOTIATE = "Negotiate";

    @Inject
    ErrataCachingKerberosClientSupport kerberosClientSupport;

    @ConfigProperty(name = "sbomer.features.kerberos.enabled", defaultValue = "false")
    private boolean enabled;

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        if (!enabled)
            return;
            
        String serviceTicket = kerberosClientSupport.getServiceTicket();
        requestContext.getHeaders().add(AUTHORIZATION, NEGOTIATE + " " + serviceTicket);
    }

}