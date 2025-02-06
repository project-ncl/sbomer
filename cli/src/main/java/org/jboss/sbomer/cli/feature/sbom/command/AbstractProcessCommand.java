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

import java.nio.file.Path;
import java.util.concurrent.Callable;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.pnc.PncService;

import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;

@Slf4j
public abstract class AbstractProcessCommand implements Callable<Integer> {

    @Inject
    protected PncService pncService;

    @Override
    public Integer call() {
        try {
            // Make sure there is no context.
            MDCUtils.removeContext();

            // Call the hook to set a context, if needed.
            addContext();

            // Fetch manifest on the given path.
            Bom bom = SbomUtils.fromPath(manifestPath());

            log.info("Starting {} processor", getImplementationType());

            // Process it.
            Bom processedBom = doProcess(bom);

            log.debug("{} processor finished", getImplementationType());

            // And save the processed manifest on the same path.
            SbomUtils.toPath(processedBom, manifestPath());

            return CommandLine.ExitCode.OK;
        } finally {
            MDCUtils.removeContext();
        }
    }

    protected abstract ProcessorType getImplementationType();

    /**
     * Optionally adds an MDC context. The {@link MDCUtils} class can be used for this purpose.
     */
    protected void addContext() {

    }

    /**
     * Processor implementation.
     *
     * @param bom the manifest to process
     * @return the processed manifest
     */
    protected abstract Bom doProcess(Bom bom);

    /**
     * Path to the CycloneDX manifest that should be processed.
     *
     * @return the path to the manifest
     */
    protected abstract Path manifestPath();
}
