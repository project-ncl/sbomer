/**
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
package org.redhat.sbomer.service;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.dto.Build;

/**
 * A service to interact with the PNC build system.
 */
@ApplicationScoped
public class PNCService {

    /**
     * Setup basic configuration to be able to talk to PNC.
     *
     * TODO: Make it configurable -- move to ConfigMap.
     *
     * @return
     */
    private Configuration getConfiguration() {
        return Configuration.builder().host("orch.psi.redhat.com").protocol("http").build();
    }

    /**
     * Fetch information about the PNC {@link Build} identified by the particular buildId.
     *
     * @param buildId
     * @return
     */
    public Build getBuild(String buildId) {
        BuildClient client = new BuildClient(getConfiguration());

        try {
            return client.getSpecific(buildId);
        } catch (RemoteResourceException e) {
            e.printStackTrace();
        }

        // TODO: Change this to throw an exception instead
        return null;

    }

}
