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

import org.jboss.sbomer.core.patch.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.feature.sbom.processor.DefaultProcessor;
import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;
import org.jboss.sbomer.core.features.sbom.utils.MDCUtils;

import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
        mixinStandardHelpOptions = true,
        name = "default",
        description = "Process the SBOM with enrichments applied to known CycloneDX fields")
public class DefaultProcessCommand extends AbstractProcessCommand {

    @ParentCommand
    @Getter
    ProcessCommand parent;

    @Inject
    DefaultProcessor defaultProcessor;

    @Override
    public ProcessorType getImplementationType() {
        return defaultProcessor.getType();
    }

    @Override
    public Bom doProcess(Bom bom) {
        return defaultProcessor.process(bom);
    }

    @Override
    protected Path manifestPath() {
        return parent.getParent().getParent().getOutput();
    }

    @Override
    protected void addContext() {
        MDCUtils.addBuildContext(parent.getParent().getParent().getBuildId());
    }
}
