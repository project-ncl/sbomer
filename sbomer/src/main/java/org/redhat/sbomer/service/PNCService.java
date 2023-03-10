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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.pnc.client.ArtifactClient;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.client.RemoteResourceNotFoundException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.redhat.sbomer.errors.ApplicationException;

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
        } catch (RemoteResourceNotFoundException ex) {
            throw new ApplicationException("Build was not found in PNC", ex);
        } catch (RemoteResourceException ex) {
            throw new ApplicationException("Build could not be retrieved because PNC responded with an error", ex);
        }
    }

    /**
     * Fetch information about the PNC {@link Artifact} identified by the particular purl.
     *
     * @param purl
     * @return
     */
    public Artifact getArtifact(String purl) {
        ArtifactClient client = new ArtifactClient(getConfiguration());

        try {
            String encodedPurl = URLEncoder.encode(purl, StandardCharsets.UTF_8);

            Iterator<Artifact> artifacts = client
                    .getAll(null, null, null, Optional.empty(), Optional.of("purl==%22" + encodedPurl + "%22"))
                    .getAll()
                    .iterator();
            if (artifacts.hasNext()) {
                return artifacts.next();
            }
        } catch (RemoteResourceNotFoundException ex) {
            throw new ApplicationException("Artifact was not found in PNC", ex);
        } catch (RemoteResourceException ex) {
            throw new ApplicationException("Artifact could not be retrieved because PNC responded with an error", ex);
        }
        return null;
    }

}
