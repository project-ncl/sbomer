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
package org.jboss.sbomer.cli.feature.sbom.command;

import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_PNC_BUILD_ID;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_BREW_BUILD_ID;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE;

import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Hash;
import org.jboss.pnc.build.finder.koji.KojiBuild;
import org.jboss.pnc.common.Strings;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.cli.feature.sbom.service.KojiService;
import org.jboss.sbomer.cli.feature.sbom.service.PncService;
import org.jboss.sbomer.cli.feature.sbom.utils.RhVersionPattern;
import org.jboss.sbomer.cli.feature.sbom.utils.buildfinder.FinderStatus;
import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "default",
        description = "Process the SBOM with enrichments applied to known CycloneDX fields")
public class DefaultProcessCommand extends AbstractProcessCommand {

    @ParentCommand
    ProcessCommand parent;

    @Inject
    protected PncService pncService;

    @Inject
    protected KojiService kojiService;

    /**
     * Performs processing for a given {@link Component}.
     *
     * @param component
     */
    protected void processComponent(Component component) {
        if (!RhVersionPattern.isRhVersion(component.getVersion())) {
            log.info("Component unknown to PNC found, purl: '{}', skipping processing", component.getPurl());
            return;
        }

        log.debug("Component with Red Hat version found, purl: {}", component.getPurl());

        SbomUtils.setPublisher(component);
        SbomUtils.setSupplier(component);
        SbomUtils.addMrrc(component);

        // If the component does not have "pnc-build-id" nor "pnc-environment-image" nor "brew-build-id", query it
        if (!SbomUtils.hasExternalReference(component, ExternalReference.Type.BUILD_SYSTEM, SBOM_RED_HAT_PNC_BUILD_ID)
                && !SbomUtils.hasExternalReference(
                        component,
                        ExternalReference.Type.BUILD_META,
                        SBOM_RED_HAT_ENVIRONMENT_IMAGE)
                && !SbomUtils.hasExternalReference(
                        component,
                        ExternalReference.Type.BUILD_SYSTEM,
                        SBOM_RED_HAT_BREW_BUILD_ID)) {

            Optional<String> sha256 = SbomUtils.getHash(component, Hash.Algorithm.SHA_256);
            Artifact artifact = pncService
                    .getArtifact(getParent().getParent().getParent().getBuildId(), component.getPurl(), sha256);

            if (artifact == null) {
                log.warn(
                        "Artifact with purl '{}' was not found in PNC, skipping processing for this artifact",
                        component.getPurl());
                return;
            }

            log.info(
                    "Starting processing of Red Hat component '{}' with PNC artifact '{}'...",
                    component.getPurl(),
                    artifact.getId());

            // Add build-related information, if we found a build in PNC
            if (artifact.getBuild() != null) {
                log.debug(
                        "Component '{}' was built in PNC, adding enrichment from PNC build '{}'",
                        component.getPurl(),
                        artifact.getBuild().getId());
                processPncBuild(component, artifact.getBuild());
            } else {
                // Lookup the build in Brew, as the artifact was found in PNC but without a build attached
                log.debug(
                        "Component '{}' was not built in PNC, will search in Brew the corresponding artifact '{}'",
                        component.getPurl(),
                        artifact.getPublicUrl());
                processBrewBuild(component, artifact);
            }
        } else {
            log.debug(
                    "Component with purl '{}' is already enriched, skipping further processing for this component",
                    component.getPurl());
        }
    }

    private void processPncBuild(Component component, Build build) {
        SbomUtils.addExternalReference(
                component,
                ExternalReference.Type.BUILD_SYSTEM,
                "https://" + pncService.getApiUrl() + "/pnc-rest/v2/builds/" + build.getId().toString(),
                SBOM_RED_HAT_PNC_BUILD_ID);

        SbomUtils.addExternalReference(
                component,
                ExternalReference.Type.BUILD_META,
                build.getEnvironment().getSystemImageRepositoryUrl() + "/" + build.getEnvironment().getSystemImageId(),
                SBOM_RED_HAT_ENVIRONMENT_IMAGE);
        if (!SbomUtils.hasExternalReference(component, ExternalReference.Type.VCS)) {
            SbomUtils.addExternalReference(
                    component,
                    ExternalReference.Type.VCS,
                    build.getScmRepository().getExternalUrl(),
                    "");
        }

        SbomUtils.addPedigreeCommit(component, build.getScmUrl() + "#" + build.getScmTag(), build.getScmRevision());
        // Reverted the code below (NCL-8171) until NCL-6938 is done
        /*
         * if (!Strings.isEmpty(build.getScmRepository().getExternalUrl()) && build.getBuildConfigRevision() != null) {
         * SbomUtils.addPedigreeCommit( component, build.getScmRepository().getExternalUrl() + "#" +
         * build.getBuildConfigRevision().getScmRevision(), build.getBuildConfigRevision().getScmRevision()); }
         */
    }

    private void processBrewBuild(Component component, Artifact artifact) {
        KojiBuild brewBuild = findBuildInBrew(artifact);
        if (brewBuild != null) {

            log.debug(
                    "Component '{}' was built in Brew, adding enrichment from Brew build '{}'",
                    component.getPurl(),
                    brewBuild.getBuildInfo().getId());

            SbomUtils.addExternalReference(
                    component,
                    ExternalReference.Type.BUILD_SYSTEM,
                    kojiService.getConfig().getKojiWebURL() + "/buildinfo?buildID=" + brewBuild.getBuildInfo().getId(),
                    SBOM_RED_HAT_BREW_BUILD_ID);

            if (brewBuild.getSource().isPresent()) {
                String scmSource = brewBuild.getSource().get();

                if (!SbomUtils.hasExternalReference(component, ExternalReference.Type.VCS)) {
                    SbomUtils.addExternalReference(component, ExternalReference.Type.VCS, scmSource, "");
                }

                int hashIndex = scmSource.lastIndexOf('#');
                if (hashIndex != -1) {
                    String commit = scmSource.substring(hashIndex + 1);
                    SbomUtils.addPedigreeCommit(component, scmSource, commit);
                }
            }
        } else {
            log.debug("Component '{}' was not built in Brew, cannot add any enrichment!", component.getPurl());
        }
    }

    private KojiBuild findBuildInBrew(Artifact artifact) {

        if (artifact.getPublicUrl() != null) {
            try {
                FinderStatus status = new FinderStatus();
                List<KojiBuild> brewBuilds = kojiService.find(artifact.getPublicUrl(), status, status);
                if (brewBuilds.size() == 1) {
                    return brewBuilds.get(0);
                } else if (brewBuilds.size() > 1) {
                    log.warn(
                            "Multiple builds where found in Brew for the artifact '{}', picking the first one!",
                            artifact.getPublicUrl());
                    return brewBuilds.get(0);
                }
            } catch (Throwable e) {
                log.error("Lookup in Brew failed due to {}", e.getMessage() == null ? e.toString() : e.getMessage(), e);
            }
        }
        return null;
    }

    @Override
    public ProcessorType getImplementationType() {
        return ProcessorType.DEFAULT;
    }

    @Override
    public Bom doProcess(Bom bom) {
        if (bom.getMetadata() != null && bom.getMetadata().getComponent() != null) {
            processComponent(bom.getMetadata().getComponent());
        }
        if (bom.getComponents() != null) {
            for (Component c : bom.getComponents()) {
                processComponent(c);
            }
        }

        return bom;
    }
}
