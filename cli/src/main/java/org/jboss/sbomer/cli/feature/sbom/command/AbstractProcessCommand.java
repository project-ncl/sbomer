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
package org.jboss.sbomer.cli.feature.sbom.command;

import java.util.concurrent.Callable;

import javax.inject.Inject;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.feature.sbom.service.PncService;
import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ParentCommand;

@Slf4j
public abstract class AbstractProcessCommand implements Callable<Integer> {

    @Getter
    @ParentCommand
    ProcessCommand parent;

    @Inject
    protected PncService pncService;

    @Override
    public Integer call() throws Exception {
        try {
            // make sure there is no context
            MDCUtils.removeContext();
            MDCUtils.addBuildContext(parent.getParent().getParent().getBuildId());

            Bom bom = SbomUtils.fromPath(parent.getParent().getParent().getOutput());

            log.info("Starting {} processor", getImplementationType());

            Bom processedBom = doProcess(bom);

            log.debug("{} processor finished", getImplementationType());

            SbomUtils.toPath(processedBom, parent.getParent().getParent().getOutput());

            return CommandLine.ExitCode.OK;
        } finally {
            MDCUtils.removeContext();
        }
    }

    public abstract ProcessorType getImplementationType();

    public abstract Bom doProcess(Bom bom);
}
