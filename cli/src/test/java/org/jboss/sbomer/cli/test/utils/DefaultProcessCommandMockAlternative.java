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
package org.jboss.sbomer.cli.test.utils;

import org.cyclonedx.model.Component;
import org.jboss.pnc.dto.Artifact;
import org.jboss.sbomer.cli.feature.sbom.command.DefaultProcessCommand;

import jakarta.enterprise.inject.Alternative;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Alternative
@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "default",
        description = "Process the SBOM with enrichments applied to known CycloneDX fields")
public class DefaultProcessCommandMockAlternative extends DefaultProcessCommand {

    @Override
    protected void processBrewBuild(Component component, Artifact artifact) {
        log.debug("Mocked component '{}' was not built in Brew, cannot add any enrichment!", component.getPurl());
    }

}
