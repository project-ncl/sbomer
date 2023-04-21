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

import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_BUILD_ID;
import static org.jboss.sbomer.core.utils.Constants.SBOM_RED_HAT_ENVIRONMENT_IMAGE;

import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.jboss.pnc.dto.Artifact;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.utils.RhVersionPattern;
import org.jboss.sbomer.core.utils.SbomUtils;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "default",
        aliases = { "def" },
        description = "Process the SBOM with enrichments applied to known CycloneDX fields")
public class DefaultProcessCommand extends AbstractBaseProcessCommand {
    @Override
    protected void processComponent(Component component) {
        if (!RhVersionPattern.isRhVersion(component.getVersion())) {
            log.info("Component unknown to PNC found, purl: '{}', skipping processing", component.getPurl());
            return;
        }

        log.info("Component with Red Hat version found, purl: {}", component.getPurl());

        Artifact artifact = pncService.getArtifact(component.getPurl());

        log.debug(
                "Starting processing of component '{}' with PNC artifact '{}'...",
                component.getPurl(),
                artifact.getId());

        SbomUtils.setPublisher(component);
        SbomUtils.setSupplier(component);
        SbomUtils.addMrrc(component);

        // Add build-related information, if we found a build in PNC
        if (artifact.getBuild() != null) {
            processBuild(component, artifact.getBuild());
        }
    }

    private void processBuild(Component component, Build build) {
        SbomUtils.addExternalReference(
                component,
                ExternalReference.Type.BUILD_SYSTEM,
                "https://" + pncService.getApiUrl() + "/pnc-rest/v2/builds/" + build.getId().toString(),
                SBOM_RED_HAT_BUILD_ID);

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
    }

    @Override
    protected ProcessorImplementation getImplementationType() {
        return ProcessorImplementation.DEFAULT;
    }
}
