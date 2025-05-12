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
package org.jboss.sbomer.cli.feature.sbom.command.catalog;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.feature.sbom.client.facade.SBOMerClientFacade;
import org.jboss.sbomer.cli.feature.sbom.utils.otel.OtelCLIUtils;
import org.jboss.sbomer.core.features.sbom.utils.FileUtils;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;

@Slf4j
public abstract class AbstractCatalogCommand implements Callable<Integer> {

    @ParentCommand
    CatalogCommand parent;

    @Inject
    SBOMerClientFacade sbomerClientFacade;

    @Override
    public Integer call() throws Exception {
        try {
            // Make sure there is no context.
            MDCUtils.removeContext();

            // Call the hook to set a context, if needed.
            addContext();
            OtelCLIUtils.startOtel("catalog-cli");

            List<Path> sbomPaths = FileUtils.findManifests(parent.getPath());
            List<Bom> boms = sbomPaths.stream().map(SbomUtils::fromPath).toList();

            log.info("Starting {} cataloguer", getCataloguerType());

            Bom indexManifest = doCatalog(parent.getImageName(), parent.getImageDigest(), boms);

            log.debug("{} cataloguer finished", getCataloguerType());

            SbomUtils.toPath(indexManifest, parent.getOutputPath());

            return CommandLine.ExitCode.OK;
        } finally {
            MDCUtils.removeContext();
            OtelCLIUtils.stopOTel();
        }
    }

    /**
     * Optionally adds an MDC context. The {@link MDCUtils} class can be used for this purpose.
     */
    protected void addContext() {
        MDCUtils.addOtelContext(OtelCLIUtils.getOtelContextFromEnvVariables());
    }

    protected String getSBOMerVersion() {
        return sbomerClientFacade.getSbomerVersion();
    }

    protected abstract String getCataloguerType();

    protected abstract Bom doCatalog(String imageName, String imageDigest, List<Bom> boms);

}
