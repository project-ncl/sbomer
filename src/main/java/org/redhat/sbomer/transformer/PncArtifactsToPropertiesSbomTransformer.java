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
package org.redhat.sbomer.transformer;

import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_BUILD_ID;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_BUILD_SYSTEM;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_ORIGIN_URL;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_PUBLIC_URL;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_SCM_EXTERNAL_URL;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_SCM_REVISION;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_SCM_TAG;
import static org.redhat.sbomer.utils.Constants.SBOM_RED_HAT_SCM_URL;
import static org.redhat.sbomer.utils.SbomUtils.addHash;
import static org.redhat.sbomer.utils.SbomUtils.addMrrc;
import static org.redhat.sbomer.utils.SbomUtils.addProperty;
import static org.redhat.sbomer.utils.SbomUtils.hasHash;
import static org.redhat.sbomer.utils.SbomUtils.hasProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Hash.Algorithm;
import org.redhat.sbomer.utils.RhVersionPattern;
import org.redhat.sbomer.dto.ArtifactInfo;
import org.redhat.sbomer.model.ArtifactCache;
import org.redhat.sbomer.service.SBOMService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class PncArtifactsToPropertiesSbomTransformer implements SbomTransformer {

    @Inject
    SBOMService sbomService;

    @Override
    public Bom transform(Bom originalBom) {
        log.info("Adding PNC cached build info to the SBOM properties");
        if (originalBom.getComponents() == null) {
            return originalBom;
        }

        for (Component c : originalBom.getComponents()) {
            if (RhVersionPattern.isRhVersion(c.getVersion())) {
                log.info("SBOM component with Red Hat version found, purl: {}", c.getPurl());
                try {
                    final ArtifactCache artifact = sbomService.fetchArtifact(c.getPurl());
                    final ArtifactInfo info = artifact.getArtifactInfo();

                    if (info.getMd5() != null && !hasHash(c.getHashes(), Algorithm.MD5)) {
                        addHash(c.getHashes(), Algorithm.MD5, info.getMd5());
                    }
                    if (info.getSha1() != null && !hasHash(c.getHashes(), Algorithm.SHA1)) {
                        addHash(c.getHashes(), Algorithm.SHA1, info.getSha1());
                    }
                    if (info.getSha256() != null && !hasHash(c.getHashes(), Algorithm.SHA_256)) {
                        addHash(c.getHashes(), Algorithm.SHA_256, info.getSha256());
                    }

                    if (!hasProperty(c.getProperties(), SBOM_RED_HAT_PUBLIC_URL)) {
                        addProperty(c.getProperties(), SBOM_RED_HAT_PUBLIC_URL, info.getPublicUrl());
                    }
                    if (!hasProperty(c.getProperties(), SBOM_RED_HAT_ORIGIN_URL)) {
                        addProperty(c.getProperties(), SBOM_RED_HAT_ORIGIN_URL, info.getOriginUrl());
                    }
                    if (!hasProperty(c.getProperties(), SBOM_RED_HAT_BUILD_ID)) {
                        addProperty(c.getProperties(), SBOM_RED_HAT_BUILD_ID, info.getBuildId());
                    }
                    if (!hasProperty(c.getProperties(), SBOM_RED_HAT_BUILD_SYSTEM)) {
                        addProperty(c.getProperties(), SBOM_RED_HAT_BUILD_SYSTEM, info.getBuildSystem());
                    }
                    if (!hasProperty(c.getProperties(), SBOM_RED_HAT_SCM_URL)) {
                        addProperty(c.getProperties(), SBOM_RED_HAT_SCM_URL, info.getScmUrl());
                    }
                    if (!hasProperty(c.getProperties(), SBOM_RED_HAT_SCM_REVISION)) {
                        addProperty(c.getProperties(), SBOM_RED_HAT_SCM_REVISION, info.getScmRevision());
                    }
                    if (!hasProperty(c.getProperties(), SBOM_RED_HAT_SCM_TAG)) {
                        addProperty(c.getProperties(), SBOM_RED_HAT_SCM_TAG, info.getScmTag());
                    }
                    if (!hasProperty(c.getProperties(), SBOM_RED_HAT_SCM_EXTERNAL_URL)) {
                        addProperty(c.getProperties(), SBOM_RED_HAT_SCM_EXTERNAL_URL, info.getScmExternalUrl());
                    }
                    if (!hasProperty(c.getProperties(), SBOM_RED_HAT_ENVIRONMENT_IMAGE)) {
                        addProperty(c.getProperties(), SBOM_RED_HAT_ENVIRONMENT_IMAGE, info.getEnvironmentImage());
                    }

                    addMrrc(c);
                } catch (NotFoundException nfe) {
                    log.warn(nfe.getMessage());
                }

            }
        }

        return originalBom;
    }

}