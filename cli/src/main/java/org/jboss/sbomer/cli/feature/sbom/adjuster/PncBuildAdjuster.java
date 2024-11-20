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
package org.jboss.sbomer.cli.feature.sbom.adjuster;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.cyclonedx.exception.GeneratorException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Dependency;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * Adjuster dedicated to manifests generated for PNC builds.
 *
 * @author Marek Goldmann
 */
@Slf4j
public class PncBuildAdjuster implements Adjuster {
    @Override
    public Bom adjust(Bom bom) {
        if (bom.getSerialNumber() == null || bom.getSerialNumber().isEmpty()) {
            log.debug("Setting 'serialNumber' for manifest with purl '{}'", bom.getMetadata().getComponent().getPurl());

            try {
                String jsonContent = SbomUtils.toJson(bom);
                bom.setSerialNumber("urn:uuid:" + UUID.nameUUIDFromBytes(jsonContent.getBytes(UTF_8)).toString());
            } catch (GeneratorException e) {
                log.warn("Could not generate serialNumber out of the manifest content, setting random UUID");
                bom.setSerialNumber(UUID.randomUUID().toString());
            }
        }

        cleanDependsOn(bom);

        return bom;
    }

    private void cleanDependsOn(Bom bom) {
        // SBOMER-236 - Dependency reference does not exist in the BOM
        if (bom.getDependencies() == null || bom.getDependencies().isEmpty()) {
            return;
        }

        Set<String> validRefs = new HashSet<>();
        for (Dependency dependency : bom.getDependencies()) {
            validRefs.add(dependency.getRef());
        }

        // Iterate through the dependencies and clean their "dependsOn" lists if it contains invalid refs
        for (Dependency dependency : bom.getDependencies()) {
            if (dependency.getDependencies() != null && !dependency.getDependencies().isEmpty()) {
                dependency.getDependencies().removeIf(dependRef -> !validRefs.contains(dependRef.getRef()));
            }
        }
    }

}
