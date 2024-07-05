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
import java.util.ArrayList;
import java.util.List;

import org.cyclonedx.model.Bom;
import org.jboss.sbomer.cli.feature.sbom.adjuster.SyftImageAdjuster;
import org.jboss.sbomer.core.features.sbom.enums.GeneratorType;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        mixinStandardHelpOptions = true,
        name = "syft-image",
        description = "Adjust the Syft manifest output for a container image")
public class SyftImageAdjustCommand extends AbstractAdjustCommand {

    @Option(names = "--path-filter")
    List<String> paths = new ArrayList<>();

    @Override
    protected GeneratorType getGeneratorType() {
        return GeneratorType.IMAGE_SYFT;
    }

    @Override
    protected Bom doAdjust(Bom bom, Path workDir) {
        SyftImageAdjuster adjuster = new SyftImageAdjuster(paths);

        return adjuster.adjust(bom, workDir);
    }
}
