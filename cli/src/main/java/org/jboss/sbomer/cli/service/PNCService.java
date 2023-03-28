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
package org.jboss.sbomer.cli.service;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.client.ArtifactClient;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.client.RemoteResourceNotFoundException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.core.errors.ApplicationException;

/**
 * A service to interact with the PNC build system.
 */
@ApplicationScoped
public class PNCService {

    @ConfigProperty(name = "sbomer.pnc-api-url")
    String apiUrl;

    /**
     * Setup basic configuration to be able to talk to PNC.
     *
     *
     * @return
     */
    protected Configuration getConfiguration() {
        return Configuration.builder().host(apiUrl).protocol("http").build();
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
            Build build = client.getSpecific(buildId);
            client.close();
            return build;
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
            String artifactQuery = "purl==\"" + purl + "\"";
            RemoteCollection<Artifact> artifacts = client
                    .getAll(null, null, null, Optional.empty(), Optional.of(artifactQuery));
            if (artifacts.size() == 0) {
                return null;
            } else if (artifacts.size() > 1) {
                throw new IllegalStateException("There should exist only one artifact with purl " + purl);
            }
            Artifact singleArtifact = artifacts.iterator().next();
            return singleArtifact;
        } catch (RemoteResourceNotFoundException ex) {
            throw new ApplicationException("Artifact was not found in PNC", ex);
        } catch (RemoteResourceException ex) {
            throw new ApplicationException("Artifact could not be retrieved because PNC responded with an error", ex);
        }
    }

}
