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
package org.jboss.sbomer.processor;

import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_BUILD_ID;
import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_BUILD_SYSTEM;
import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE;
import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_ORIGIN_URL;
import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_PUBLIC_URL;
import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_SCM_EXTERNAL_URL;
import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_SCM_REVISION;
import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_SCM_TAG;
import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_SCM_URL;
import static org.jboss.sbomer.core.utils.SbomUtils.addHash;
import static org.jboss.sbomer.core.utils.SbomUtils.addMrrc;
import static org.jboss.sbomer.core.utils.SbomUtils.addProperty;
import static org.jboss.sbomer.core.utils.SbomUtils.hasHash;
import static org.jboss.sbomer.core.utils.SbomUtils.hasProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.ValidationException;
import javax.ws.rs.NotFoundException;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Hash.Algorithm;
import org.jboss.sbomer.core.utils.RhVersionPattern;
import org.jboss.sbomer.dto.ArtifactInfo;
import org.jboss.sbomer.model.Sbom;
import org.jboss.sbomer.service.SBOMService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@PncToSbomProperties
@ApplicationScoped
public class PncArtifactsToSbomPropertiesProcessor implements SbomProcessor {

    @Inject
    SBOMService sbomService;

    @Override
    public Bom process(Bom originalBom) {
        log.info(
                "Applying SBOM_PROPERTIES processing to the SBOM: {}",
                originalBom.getMetadata().getComponent().getPurl());

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
                final ArtifactInfo info = sbomService.fetchArtifact(component.getPurl());

                if (info.getMd5() != null && !hasHash(component, Algorithm.MD5)) {
                    addHash(component, Algorithm.MD5, info.getMd5());
                }
                if (info.getSha1() != null && !hasHash(component, Algorithm.SHA1)) {
                    addHash(component, Algorithm.SHA1, info.getSha1());
                }
                if (info.getSha256() != null && !hasHash(component, Algorithm.SHA_256)) {
                    addHash(component, Algorithm.SHA_256, info.getSha256());
                }

                if (!hasProperty(component, SBOM_RED_HAT_PUBLIC_URL)) {
                    addProperty(component, SBOM_RED_HAT_PUBLIC_URL, info.getPublicUrl());
                }
                if (!hasProperty(component, SBOM_RED_HAT_ORIGIN_URL)) {
                    addProperty(component, SBOM_RED_HAT_ORIGIN_URL, info.getOriginUrl());
                }
                if (!hasProperty(component, SBOM_RED_HAT_BUILD_ID)) {
                    addProperty(component, SBOM_RED_HAT_BUILD_ID, info.getBuildId());
                }
                if (!hasProperty(component, SBOM_RED_HAT_BUILD_SYSTEM)) {
                    addProperty(component, SBOM_RED_HAT_BUILD_SYSTEM, info.getBuildSystem());
                }
                if (!hasProperty(component, SBOM_RED_HAT_SCM_URL)) {
                    addProperty(component, SBOM_RED_HAT_SCM_URL, info.getScmUrl());
                }
                if (!hasProperty(component, SBOM_RED_HAT_SCM_REVISION)) {
                    addProperty(component, SBOM_RED_HAT_SCM_REVISION, info.getScmRevision());
                }
                if (!hasProperty(component, SBOM_RED_HAT_SCM_TAG)) {
                    addProperty(component, SBOM_RED_HAT_SCM_TAG, info.getScmTag());
                }
                if (!hasProperty(component, SBOM_RED_HAT_SCM_EXTERNAL_URL)) {
                    addProperty(component, SBOM_RED_HAT_SCM_EXTERNAL_URL, info.getScmExternalUrl());
                }
                if (!hasProperty(component, SBOM_RED_HAT_ENVIRONMENT_IMAGE)) {
                    addProperty(component, SBOM_RED_HAT_ENVIRONMENT_IMAGE, info.getEnvironmentImage());
                }

                addMrrc(component);
            } catch (NotFoundException nfe) {
                log.warn(nfe.getMessage());
            }
        }
    }

}