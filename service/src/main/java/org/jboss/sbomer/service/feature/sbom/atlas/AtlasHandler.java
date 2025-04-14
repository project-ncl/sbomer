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
package org.jboss.sbomer.service.feature.sbom.atlas;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.errors.ClientException;
import org.jboss.sbomer.service.feature.FeatureFlags;
import org.jboss.sbomer.service.feature.errors.FeatureDisabledException;
import org.jboss.sbomer.service.feature.sbom.model.Sbom;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Setter
@ApplicationScoped
@Slf4j
public class AtlasHandler {

    @Inject
    @RestClient
    AtlasBuildClient atlasBuildClient;

    @Inject
    @RestClient
    AtlasReleaseClient atlasReleaseClient;

    @Inject
    FeatureFlags featureFlags;

    public void publishBuildManifests(List<Sbom> sboms) {
        publishManifests(sboms, false);
    }

    public void publishReleaseManifest(Sbom sbom) {
        publishManifests(Collections.singletonList(sbom), true);
    }

    private void publishManifests(List<Sbom> sboms, boolean isRelease) {
        if (sboms == null) {
            log.warn(
                    "Manifest list is not provided, this is unexpected, Atlas will not be populated with manifest, but continuing");
            return;
        }

        if (sboms.isEmpty()) {
            log.warn("No manifests provided to upload to Atlas, nothing will be published");
            return;
        }

        if (!featureFlags.atlasPublish()) {
            throw new FeatureDisabledException(
                    "Atlas integration is disabled, following manifests will not be published to Atlas: {}",
                    sboms.stream().map(Sbom::getId).collect(Collectors.joining(", ")));
        }

        AtlasClient atlasClient = isRelease ? atlasReleaseClient : atlasBuildClient;
        String atlasInstanceName = isRelease ? "release" : "build";
            log.info("Uploading {} {} manifests...", sboms.size(), atlasInstanceName);

        for (Sbom sbom : sboms) {
            uploadManifest(sbom, atlasClient);
        }

        log.info("Upload complete!");
    }

    private void uploadManifest(Sbom sbom, AtlasClient atlasClient) {
        log.info("Uploading manifest '{}' (purl: '{}')...", sbom.getId(), sbom.getRootPurl());

        try {
            // Store it!
            atlasClient.upload(Map.of("type", "cyclonedx"), sbom.getSbom());
        } catch (ClientException e) {
            throw new ApplicationException(
                    "Unable to store '{}' manifest in Atlas, purl: '{}': {}",
                    sbom.getId(),
                    sbom.getRootPurl(),
                    e.getMessage(),
                    e);
        }

        log.info("Manifest {} uploaded!", sbom.getId());
    }
}
