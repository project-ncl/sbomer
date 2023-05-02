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
package org.jboss.sbomer.core.service;

import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.client.ArtifactClient;
import org.jboss.pnc.client.BuildClient;
import org.jboss.pnc.client.BuildConfigurationClient;
import org.jboss.pnc.client.Configuration;
import org.jboss.pnc.client.RemoteCollection;
import org.jboss.pnc.client.RemoteResourceException;
import org.jboss.pnc.client.RemoteResourceNotFoundException;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.pnc.dto.ProductVersionRef;
import org.jboss.sbomer.core.errors.ApplicationException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * A service to interact with the PNC build system.
 */
@Slf4j
@ApplicationScoped
public class PncService {

    @ConfigProperty(name = "sbomer.pnc.api-url")
    @Getter
    String apiUrl;

    ArtifactClient artifactClient;

    BuildClient buildClient;

    BuildConfigurationClient buildConfigurationClient;

    @PostConstruct
    void init() {
        artifactClient = new ArtifactClient(getConfiguration());
        buildClient = new BuildClient(getConfiguration());
        buildConfigurationClient = new BuildConfigurationClient(getConfiguration());
    }

    @PreDestroy
    void cleanup() {
        artifactClient.close();
        buildClient.close();
        buildConfigurationClient.close();
    }

    /**
     * Setup basic configuration to be able to talk to PNC.
     *
     *
     * @return
     */
    public Configuration getConfiguration() {
        return Configuration.builder().host(apiUrl).protocol("http").build();
    }

    /**
     * <p>
     * Fetch information about the PNC {@link Build} identified by the particular {@code buildId}.
     * </p>
     *
     * <p>
     * In case the {@link Build} with provided identifier cannot be found {@code null} is returned.
     * </p>
     *
     * @param buildId Tbe {@link Build} identifier in PNC
     * @return The {@link Build} object or {@code null} in case the {@link Build} could not be found.
     */
    public Build getBuild(String buildId) {
        log.debug("Fetching Build from PNC with id '{}'", buildId);
        try {
            return buildClient.getSpecific(buildId);
        } catch (RemoteResourceNotFoundException ex) {
            log.warn("Build with id '{}' was not found in PNC", buildId);
            return null;
        } catch (RemoteResourceException ex) {
            throw new ApplicationException("Build could not be retrieved because PNC responded with an error", ex);
        }
    }

    /**
     * <p>
     * Fetch information about the PNC {@link BuildConfiguration} identified by the particular {@code buildConfigId}.
     * </p>
     *
     * <p>
     * In case the {@link BuildConfiguration} with provided identifier cannot be found {@code null} is returned.
     * </p>
     *
     * @param buildId Tbe {@link BuildConfiguration} identifier in PNC
     * @return The {@link BuildConfiguration} object or {@code null} in case the {@link BuildConfiguration} could not be
     *         found.
     */
    public BuildConfiguration getBuildConfig(String buildConfigId) {
        log.debug("Fetching BuildConfig from PNC with id '{}'", buildConfigId);
        try {
            return buildConfigurationClient.getSpecific(buildConfigId);
        } catch (RemoteResourceNotFoundException ex) {
            log.warn("BuildConfig with id '{}' was not found in PNC", buildConfigId);
            return null;
        } catch (RemoteResourceException ex) {
            throw new ApplicationException(
                    "BuildConfig could not be retrieved because PNC responded with an error",
                    ex);
        }
    }

    /**
     * <p>
     * Obtains the {@link ProductVersionRef} for a given PNC {@link Build} identifier.
     * <p>
     *
     * @param buildId The {@link Build} identifier to get the Product Version for.
     * @return The {@link ProductVersionRef} object for the related or {@code null} in case it is not possible to obtain
     *         it.
     */
    public ProductVersionRef getProductVersion(String buildId) {
        log.debug("Fetching Product Version information from PNC for build '{}'", buildId);

        if (buildId == null) {
            return null;
        }
        Build build = getBuild(buildId);

        if (build == null) {
            log.warn("Build related to the SBOM could not be found in PNC, interrupting processing");
            return null;
        }

        BuildConfiguration buildConfig = getBuildConfig(build.getBuildConfigRevision().getId());

        if (buildConfig == null) {
            log.warn("BuildConfig related to the SBOM could not be found in PNC, interrupting processing");
            return null;
        }

        ProductVersionRef productVersion = buildConfig.getProductVersion();

        if (productVersion == null) {
            log.warn(
                    "BuildConfig related to the SBOM does not provide product version information, interrupting processing");
            return null;
        }

        return productVersion;
    }

    /**
     * Fetch information about the PNC {@link Artifact} identified by the particular purl.
     *
     * @param purl
     * @return The {@link Artifact} object or {@code null} if it cannot be found.
     */
    public Artifact getArtifact(String purl) {
        log.debug("Fetching artifact from PNC for purl '{}'", purl);

        try {
            String artifactQuery = "purl==\"" + purl + "\"";
            RemoteCollection<Artifact> artifacts = artifactClient
                    .getAll(null, null, null, Optional.empty(), Optional.of(artifactQuery));
            if (artifacts.size() == 0) {
                log.debug("Artifact with purl '{}' was not found in PNC", purl);
                return null;
            } else if (artifacts.size() > 1) {
                throw new IllegalStateException("There should exist only one artifact with purl " + purl);
            }
            return artifacts.iterator().next();
        } catch (RemoteResourceNotFoundException ex) {
            throw new ApplicationException("Artifact with purl '{}' was not found in PNC", purl, ex);
        } catch (RemoteResourceException ex) {
            throw new ApplicationException(
                    "Artifact with purl '{}' could not be retrieved because PNC responded with an error",
                    purl,
                    ex);
        }
    }

}
