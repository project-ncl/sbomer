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
package org.redhat.sbomer.processor;

import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_BUILD_ID;
import static org.redhat.sbomer.utils.Constants.DISTRIBUTION;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE;
import static org.redhat.sbomer.utils.SbomUtils.addExternalReference;
import static org.redhat.sbomer.utils.SbomUtils.hasExternalReference;
import static org.redhat.sbomer.utils.SbomUtils.setPublisher;
import static org.redhat.sbomer.utils.SbomUtils.setSupplier;
import static org.redhat.sbomer.utils.SbomUtils.addPedigreeCommit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ValidationException;
import javax.ws.rs.NotFoundException;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.jboss.util.Strings;
import org.redhat.sbomer.utils.Constants;
import org.redhat.sbomer.utils.RhVersionPattern;
import org.redhat.sbomer.dto.ArtifactInfo;
import org.redhat.sbomer.model.ArtifactCache;
import org.redhat.sbomer.model.Sbom;
import org.redhat.sbomer.service.SBOMService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@PncToSbomPedigree
@ApplicationScoped
public class PncArtifactsToSbomPedigreeProcessor implements SbomProcessor {

    @Inject
    SBOMService sbomService;

    @Override
    public Bom process(Sbom originalSbom) {
        log.info("Applying SBOM_PEDIGREE processing to the SBOM: {}", originalSbom);

        Bom originalBom = originalSbom.getCycloneDxBom();
        if (originalBom == null) {
            throw new ValidationException("Could not convert initial SBOM of build: " + originalSbom.getBuildId());
        }

        if (originalBom.getMetadata() != null && originalBom.getMetadata().getComponent() != null) {
            processComponent(originalBom.getMetadata().getComponent());
        }
        if (originalBom.getComponents() != null) {
            for (Component c : originalBom.getComponents()) {
                processComponent(c);
            }
        }
        return originalBom;
    }

    private void processComponent(Component component) {
        if (RhVersionPattern.isRhVersion(component.getVersion())) {
            log.info("SBOM component with Red Hat version found, purl: {}", component.getPurl());
            try {
                final ArtifactCache artifact = sbomService.fetchArtifact(component.getPurl());
                final ArtifactInfo info = artifact.getArtifactInfo();

                // TODO: make url configurable
                addExternalReference(
                        component,
                        ExternalReference.Type.BUILD_SYSTEM,
                        "https://orch.psi.redhat.com/pnc-rest/v2/builds/" + info.getBuildId(),
                        SBOM_RED_HAT_BUILD_ID);
                addExternalReference(component, ExternalReference.Type.DISTRIBUTION, Constants.MRRC_URL, DISTRIBUTION);
                addExternalReference(
                        component,
                        ExternalReference.Type.BUILD_META,
                        info.getEnvironmentImage(),
                        SBOM_RED_HAT_ENVIRONMENT_IMAGE);
                if (!hasExternalReference(component, ExternalReference.Type.VCS)
                        && !Strings.isEmpty(info.getScmExternalUrl())) {
                    addExternalReference(component, ExternalReference.Type.VCS, info.getScmExternalUrl(), "");
                }

                setPublisher(component);
                setSupplier(component);
                addPedigreeCommit(component, info.getScmUrl() + "#" + info.getScmTag(), info.getScmRevision());

            } catch (NotFoundException nfe) {
                log.warn(nfe.getMessage());
            }
        }
    }

}
