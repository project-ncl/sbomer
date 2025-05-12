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

import org.jboss.pnc.dto.DeliverableAnalyzerOperation;
import org.jboss.sbomer.cli.feature.sbom.client.facade.SBOMerClientFacade;
import org.jboss.sbomer.cli.feature.sbom.command.mixin.GeneratorToolMixin;
import org.jboss.sbomer.cli.feature.sbom.service.KojiService;
import org.jboss.sbomer.cli.feature.sbom.utils.otel.OtelCLIUtils;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;
import org.jboss.sbomer.core.pnc.PncService;

import jakarta.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.ParentCommand;

@Slf4j
public abstract class AbstractGenerateOperationCommand implements Callable<Integer> {
    @Mixin
    GeneratorToolMixin generator;

    @Getter
    @ParentCommand
    GenerateOperationCommand parent;

    @Inject
    protected PncService pncService;

    @Inject
    protected KojiService kojiService;

    @Inject
    protected SBOMerClientFacade sbomerClientFacade;

    /**
     * <p>
     * Implementation of the SBOM generation for Deliverable Analyzer operations.
     * </p>
     *
     * @return a {@link Path} to the generated BOM file.
     */
    protected abstract Path doGenerate();

    protected abstract GeneratorType generatorType();

    @Override
    public Integer call() {
        try {
            // Make sure there is no context
            MDCUtils.removeContext();
            MDCUtils.addOtelContext(OtelCLIUtils.getOtelContextFromEnvVariables());

            OtelCLIUtils.startOtel("CLI.generateOperation");

            // Fetch operation information
            DeliverableAnalyzerOperation operation = pncService
                    .getDeliverableAnalyzerOperation(parent.getOperationId());

            if (operation == null) {
                log.error("Could not fetch the PNC operation with id '{}'", parent.getOperationId());
                return CommandLine.ExitCode.SOFTWARE;
            }

            Path sbomPath = doGenerate();
            log.info("Generation finished, SBOM available at: '{}'", sbomPath.toAbsolutePath());
            return 0;
        } finally {
            MDCUtils.removeContext();
            OtelCLIUtils.stopOTel();
        }
    }

}
