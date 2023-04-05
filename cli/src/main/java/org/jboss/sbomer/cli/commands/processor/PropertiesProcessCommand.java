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
package org.jboss.sbomer.cli.commands.processor;

import org.cyclonedx.model.Component;
import org.cyclonedx.model.Hash.Algorithm;
import org.jboss.pnc.dto.Artifact;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.utils.Constants;
import org.jboss.sbomer.core.utils.SbomUtils;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "properties",
        aliases = { "props" },
        description = "Process the SBOM with enrichments added to the properties")
public class PropertiesProcessCommand extends AbstractBaseProcessCommand {

    @Override
    protected void processComponentWithArtifact(Component component, Artifact artifact) {
        log.info("SBOM component with Red Hat version found, purl: {}", component.getPurl());

        if (artifact.getMd5() != null && !SbomUtils.hasHash(component, Algorithm.MD5)) {
            SbomUtils.addHash(component, Algorithm.MD5, artifact.getMd5());
        }
        if (artifact.getSha1() != null && !SbomUtils.hasHash(component, Algorithm.SHA1)) {
            SbomUtils.addHash(component, Algorithm.SHA1, artifact.getSha1());
        }
        if (artifact.getSha256() != null && !SbomUtils.hasHash(component, Algorithm.SHA_256)) {
            SbomUtils.addHash(component, Algorithm.SHA_256, artifact.getSha256());
        }

        if (!SbomUtils.hasProperty(component, Constants.SBOM_RED_HAT_PUBLIC_URL)) {
            SbomUtils.addProperty(component, Constants.SBOM_RED_HAT_PUBLIC_URL, artifact.getPublicUrl());
        }
        if (!SbomUtils.hasProperty(component, Constants.SBOM_RED_HAT_ORIGIN_URL)) {
            SbomUtils.addProperty(component, Constants.SBOM_RED_HAT_ORIGIN_URL, artifact.getOriginUrl());
        }
        if (!SbomUtils.hasProperty(component, Constants.SBOM_RED_HAT_BUILD_ID)) {
            SbomUtils.addProperty(component, Constants.SBOM_RED_HAT_BUILD_ID, artifact.getBuild().getId().toString());
        }
        // TODO: What should be the identifier here?
        // if (!SbomUtils.hasProperty(component, Constants.SBOM_RED_HAT_BUILD_SYSTEM)) {
        // SbomUtils.addProperty(component, Constants.SBOM_RED_HAT_BUILD_SYSTEM, artifact.getBuildSystem());
        // }
        if (!SbomUtils.hasProperty(component, Constants.SBOM_RED_HAT_SCM_URL)) {
            SbomUtils.addProperty(component, Constants.SBOM_RED_HAT_SCM_URL, artifact.getBuild().getScmUrl());
        }
        if (!SbomUtils.hasProperty(component, Constants.SBOM_RED_HAT_SCM_REVISION)) {
            SbomUtils.addProperty(component, Constants.SBOM_RED_HAT_SCM_REVISION, artifact.getBuild().getScmRevision());
        }
        if (!SbomUtils.hasProperty(component, Constants.SBOM_RED_HAT_SCM_TAG)) {
            SbomUtils.addProperty(component, Constants.SBOM_RED_HAT_SCM_TAG, artifact.getBuild().getScmTag());
        }
        if (!SbomUtils.hasProperty(component, Constants.SBOM_RED_HAT_SCM_EXTERNAL_URL)) {
            SbomUtils.addProperty(
                    component,
                    Constants.SBOM_RED_HAT_SCM_EXTERNAL_URL,
                    artifact.getBuild().getScmRepository().getExternalUrl());
        }
        if (!SbomUtils.hasProperty(component, Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE)) {
            SbomUtils.addProperty(
                    component,
                    Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE,
                    artifact.getBuild().getEnvironment().getSystemImageRepositoryUrl() + "/"
                            + artifact.getBuild().getEnvironment().getSystemImageId());
        }

        SbomUtils.addMrrc(component);

    }

    @Override
    protected ProcessorImplementation getImplementationType() {
        return ProcessorImplementation.PROPERTIES;
    }

}
