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

import org.jboss.sbomer.core.patch.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.jboss.sbomer.core.patch.cyclonedx.model.Dependency;

import lombok.extern.slf4j.Slf4j;

/**
 * Adjuster dedicated to manifests generated for PNC operations.
 *
 */
@Slf4j
public class PncOperationAdjuster extends AbstractAdjuster {
    @Override
    public Bom adjust(Bom bom) {
        addMissingSerialNumber(bom);

        // Remove the dependencies which depends on the same parent bom-ref (SBOMER-245)
        if (bom.getDependencies() != null) {
            for (Dependency dependency : bom.getDependencies()) {
                String parentRef = dependency.getRef();

                // Get the dependsOn list and remove entries that match the parent ref
                if (dependency.getDependencies() != null && !dependency.getDependencies().isEmpty()) {
                    dependency.getDependencies().removeIf(dependRef -> dependRef.getRef().equals(parentRef));
                }
            }
        }
        cleanupComponents(bom);

        return bom;
    }

    @Override
    protected void cleanupComponent(Component component) {
        cleanupExternalReferences(component.getExternalReferences());
    }
}
