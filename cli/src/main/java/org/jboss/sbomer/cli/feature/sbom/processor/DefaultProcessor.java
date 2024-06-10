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
package org.jboss.sbomer.cli.feature.sbom.processor;

import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_BREW_BUILD_ID;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_PNC_BUILD_ID;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.addMrrc;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.getHash;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.hasExternalReference;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.setArtifactMetadata;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.setBrewBuildMetadata;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.setPncBuildMetadata;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.setPublisher;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.setSupplier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Hash;
import org.jboss.pnc.build.finder.koji.KojiBuild;
import org.jboss.pnc.dto.Artifact;
import org.jboss.sbomer.cli.feature.sbom.service.KojiService;
import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;
import org.jboss.sbomer.core.features.sbom.utils.RhVersionPattern;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.pnc.PncService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class DefaultProcessor implements Processor {

    @Inject
    protected PncService pncService;

    @Inject
    protected KojiService kojiService;

    private Map<String, String> purlRelocations = new HashMap<>();

    /**
     * Performs processing for a given {@link Component}.
     *
     * @param component
     */
    protected void processComponent(Bom bom, Component component) {
        log.debug("Processing '{}'...", component.getPurl());

        if (component.getPurl() == null) {
            log.info("Component '{}' does not have a purl", component.getName());
            return;
        }

        if (component.getVersion() == null) {
            log.info("Component '{}' does not have a version", component.getName());
            return;
        }

        // If the component does not have "pnc-build-id" nor "pnc-environment-image" nor "brew-build-id", query it
        if (!hasExternalReference(component, ExternalReference.Type.BUILD_SYSTEM, SBOM_RED_HAT_PNC_BUILD_ID)
                && !hasExternalReference(component, ExternalReference.Type.BUILD_META, SBOM_RED_HAT_ENVIRONMENT_IMAGE)
                && !hasExternalReference(component, ExternalReference.Type.BUILD_SYSTEM, SBOM_RED_HAT_BREW_BUILD_ID)) {

            Optional<String> sha256 = getHash(component, Hash.Algorithm.SHA_256);
            Optional<String> sha1 = getHash(component, Hash.Algorithm.SHA1);
            Optional<String> md5 = getHash(component, Hash.Algorithm.MD5);

            // First try to look up the artifact with the purl given and with optional SHA256 hash to filter out results
            // Even though we may have different hashes, we specifically specify only SHA256 here.
            Artifact artifact = pncService.getArtifact(component.getPurl(), sha256, Optional.empty(), Optional.empty());

            // Artifact wasn't found, we will try lookup using different methods
            if (artifact == null) {
                log.debug(
                        "Artifact with purl '{}' wasn't found in PNC, using different methods to receive it",
                        component.getPurl());

                if (hasAnyHash(sha256, sha1, md5)) {
                    log.debug(
                            "There is at least one hash available: sha256 '{}', sha1 '{}', md5 '{}'",
                            sha256.orElse(null),
                            sha1.orElse(null),
                            md5.orElse(null));

                    log.debug("Looking up '{}' artifact in PNC using hashes only", component.getPurl());

                    // Let's try a lookup with hashes only, because the generated purl can be wrongly constructed
                    artifact = pncService.getArtifact(null, sha256, sha1, md5);
                }

                // No luck, let's try to see if we can find hashes in build-meta external references
                if (artifact == null) {
                    log.debug(
                            "Artifact with purl '{}' wasn't found in PNC using hash-only lookup, giving up",
                            component.getPurl());
                    return;
                }

                String oldPurl = component.getPurl();
                String newPurl = artifact.getPurl();

                if (newPurl == null) {
                    log.warn(
                            "The PNC artifact '{}' does not have a purl set, we cannot update the purl in the component, leaving it as is: '{}'",
                            artifact.getId(),
                            oldPurl);
                } else {
                    // It looks like we found the artifact with a hash-only query, but the purl query failed on us.
                    // This means that the purl most probably is incorrect in the manifest, so let's update it.
                    log.debug("Updating component's purl from '{}' to '{}'", oldPurl, newPurl);
                    component.setPurl(newPurl);

                    purlRelocations.put(oldPurl, newPurl);
                }
            }

            log.info(
                    "Starting processing of Red Hat component '{}' with PNC artifact '{}'...",
                    component.getPurl(),
                    artifact.getId());

            // In case this is a RH artifact, add more properties.
            if (RhVersionPattern.isRhVersion(component.getVersion())
                    || RhVersionPattern.isRhPurl(component.getPurl())) {
                setPublisher(component);
                setSupplier(component);
                addMrrc(component);
            }

            // Add artifact metadata (PNC url)
            setArtifactMetadata(component, artifact, pncService.getApiUrl());

            // Add build-related information, if we found a build in PNC
            if (artifact.getBuild() != null) {
                log.debug(
                        "Component '{}' was built in PNC, adding enrichment from PNC build '{}'",
                        component.getPurl(),
                        artifact.getBuild().getId());

                setPncBuildMetadata(component, artifact.getBuild(), pncService.getApiUrl());
            } else {
                if (RhVersionPattern.isRhVersion(component.getVersion())) {
                    // Lookup the build in Brew, as the artifact was found in PNC but without a build attached
                    log.debug(
                            "Component '{}' was not built in PNC, will search in Brew the corresponding artifact '{}'",
                            component.getPurl(),
                            artifact.getPublicUrl());
                    processBrewBuild(component, artifact);
                } else {
                    log.warn(
                            "Component '{}' was not built in PNC nor it is a RH artifact, this component won't be enriched, skipping",
                            component.getPurl());
                }
            }
        } else {
            log.debug(
                    "Component with purl '{}' is already enriched, skipping further processing for this component",
                    component.getPurl());
        }
    }

    private boolean hasAnyHash(Optional<String> sha256, Optional<String> sha1, Optional<String> md5) {
        return (sha256.orElse(null) != null || sha1.orElse(null) != null || md5.orElse(null) != null);
    }

    protected void processBrewBuild(Component component, Artifact artifact) {
        KojiBuild brewBuild = kojiService.findBuild(artifact);
        if (brewBuild != null) {

            log.debug(
                    "Component '{}' was built in Brew, adding enrichment from Brew build '{}'",
                    component.getPurl(),
                    brewBuild.getBuildInfo().getId());

            setBrewBuildMetadata(
                    component,
                    String.valueOf(brewBuild.getBuildInfo().getId()),
                    brewBuild.getSource(),
                    kojiService.getConfig().getKojiWebURL().toString());
        } else {
            log.debug("Component '{}' was not built in Brew, cannot add any enrichment!", component.getPurl());
        }
    }

    @Override
    public Bom process(Bom bom) {
        if (bom.getMetadata() != null && bom.getMetadata().getComponent() != null) {
            processComponent(bom, bom.getMetadata().getComponent());
        }
        if (bom.getComponents() != null) {
            for (Component c : bom.getComponents()) {
                processComponent(bom, c);
            }
        }

        // If there are any purl relcoations, process these.
        purlRelocations.forEach((oldPurl, newPurl) -> {
            SbomUtils.updatePurl(bom, oldPurl, newPurl);
        });

        return bom;
    }

    @Override
    public ProcessorType getType() {
        return ProcessorType.DEFAULT;
    }

}
