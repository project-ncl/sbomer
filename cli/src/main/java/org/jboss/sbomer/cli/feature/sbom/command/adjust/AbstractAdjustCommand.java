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
package org.jboss.sbomer.cli.feature.sbom.command.adjust;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Callable;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.feature.sbom.utils.otel.OtelCLIUtils;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.OtelHelper;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;

@Slf4j
public abstract class AbstractAdjustCommand implements Callable<Integer> {

    @ParentCommand
    AdjustCommand parent;

    @Override
    public Integer call() {
        try {
            // Make sure there is no context.
            MDCUtils.removeContext();

            // Call the hook to set a context, if needed.
            addContext();

            Map<String, String> attributes = Map.of(
                    "params.adjuster.type",
                    getAdjusterType(),
                    "params.source",
                    parent.getPath().toFile().getAbsolutePath(),
                    "params.destination",
                    parent.getOutputPath().toFile().getAbsolutePath());
            OtelCLIUtils.startOtel(
                    OtelCLIUtils.SBOMER_CLI_NAME,
                    OtelHelper.getEffectiveClassName(this.getClass()) + ".adjust",
                    attributes);

            Bom bom = SbomUtils.fromPath(parent.getPath());

            log.info("Starting {} adjuster", getAdjusterType());

            Bom adjustedBom = doAdjust(bom, parent.getPath().getParent());

            log.debug("{} adjuster finished", getAdjusterType());

            SbomUtils.toPath(adjustedBom, parent.getOutputPath());

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

    protected abstract String getAdjusterType();

    protected abstract Bom doAdjust(Bom bom, Path workDir);

}
