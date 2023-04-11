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

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.pnc.dto.Artifact;
import org.jboss.sbomer.cli.CLI;
import org.jboss.sbomer.cli.client.SBOMerClient;
import org.jboss.sbomer.cli.model.Sbom;
import org.jboss.sbomer.cli.service.PNCService;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.utils.RhVersionPattern;
import org.jboss.sbomer.core.utils.SbomUtils;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;

@Slf4j
public abstract class AbstractBaseProcessCommand implements Callable<Integer> {
    @Inject
    CLI cli;

    @Inject
    @RestClient
    SBOMerClient sbomerClient;

    @Inject
    PNCService pncService;

    @ParentCommand
    ProcessCommand parent;

    @Override
    public Integer call() throws Exception {
        Sbom sbom = sbomerClient.getById(parent.getSbomId());
        Bom bom = SbomUtils.fromJsonNode(sbom.getSbom());

        if (bom == null) {
            throw new ApplicationException("Invalid CycloneDX SBOM received from the '{}' ID", sbom.getId());
        }

        // Run the actual processing
        Bom processedBom = doProcess(bom);

        // Create the new SBOM based on the old one with updated fields.
        Sbom newSbom = processed(sbom, processedBom);

        // Save the SBOM in the service
        sbomerClient.save(newSbom);

        return CommandLine.ExitCode.OK;
    }

    private Sbom processed(Sbom originalSbom, Bom bom) {
        Sbom processed = new Sbom();
        processed.setBuildId(originalSbom.getBuildId());
        processed.setGenerator(originalSbom.getGenerator());
        processed.setProcessor(getImplementationType());
        processed.setType(originalSbom.getType());
        processed.setSbom(SbomUtils.toJsonNode(bom));

        return processed;
    }

    protected abstract ProcessorImplementation getImplementationType();

    protected Bom doProcess(Bom bom) {
        log.info(
                "Applying {} processing to the SBOM: {}",
                getImplementationType(),
                bom.getMetadata().getComponent().getPurl());

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

    private void processComponent(Component component) {
        if (RhVersionPattern.isRhVersion(component.getVersion())) {
            log.info("SBOM component with Red Hat version found, purl: {}", component.getPurl());

            Artifact artifact = null;

            try {
                artifact = pncService.getArtifact(component.getPurl());
            } catch (ApplicationException e) {
                log.warn(e.getMessage());
                return;
            }

            log.debug(
                    "Processing Component '{}' with PNC artifact '{}' from Build '{}'",
                    component.getPurl(),
                    artifact.getId(),
                    artifact.getBuild().getId());
            processComponentWithArtifact(component, artifact);
        }
    }

    protected abstract void processComponentWithArtifact(Component component, Artifact artifact);

}
