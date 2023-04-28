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

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.commands.AbstractCommand;
import org.jboss.sbomer.cli.model.Sbom;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.utils.SbomUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;

@Slf4j
public abstract class AbstractProcessCommand extends AbstractCommand {

    @Getter
    @ParentCommand
    ProcessCommand parent;

    @Override
    public Integer call() throws Exception {
        log.debug("Fetching SBOM with id '{}' from SBOMer...", parent.getSbomMixin().getSbomId());
        Sbom sbom = sbomerClient.getById(parent.getSbomMixin().getSbomId());

        if (sbom.getParentSbom() == null) {
            throw new ApplicationException("Requested SBOM (id: '{}') does not have a parent SBOM", sbom.getId());
        }

        log.debug("Starting {} processor", getImplementationType());

        Bom processedBom;

        // In case the SBOM is null, it means that this is an initial processing of the SBOM and because of this, the
        // relevant CycloneDX Bom is available in the parent SBOM only. Future processing will be done on the actual
        // object, because the BOM will be populated.
        if (sbom.getSbom() == null) {
            // log.debug("BOM missing, processing base BOM");
            processedBom = doProcess(sbom.getParentSbom());
        } else {
            processedBom = doProcess(sbom);
        }

        log.debug("{} processor finished", getImplementationType());

        sbomerClient.updateSbom(String.valueOf(sbom.getId()), SbomUtils.toJsonNode(processedBom));

        log.info("SBOM with id '{}' updated!", sbom.getId());

        return CommandLine.ExitCode.OK;
    }

    protected Bom getBom(Sbom sbom) {
        Bom bom = SbomUtils.fromJsonNode(sbom.getSbom());

        if (bom == null) {
            throw new ApplicationException("No CycloneDX SBOM received from the '{}' ID", sbom.getId());
        }

        return bom;
    }

    public abstract ProcessorImplementation getImplementationType();

    public abstract Bom doProcess(Sbom bom);
}
