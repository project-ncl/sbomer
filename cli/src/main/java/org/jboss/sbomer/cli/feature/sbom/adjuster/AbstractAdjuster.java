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

import lombok.extern.slf4j.Slf4j;
import org.cyclonedx.exception.GeneratorException;
import org.jboss.sbomer.core.patch.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.jboss.pnc.common.Strings;
import org.jboss.sbomer.cli.feature.sbom.utils.UriValidator;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import java.util.List;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public abstract class AbstractAdjuster implements Adjuster {

    protected void cleanupComponents(Bom bom) {
        // Cleanup main component
        cleanupComponent(bom.getMetadata().getComponent());
        cleanupComponents(bom.getMetadata().getComponent().getComponents());

        // ...and all other components
        cleanupComponents(bom.getComponents());
    }

    /**
     * Adjusts all provided {@link Component}s according to our standards.
     *
     * @param components the components
     */
    private void cleanupComponents(List<Component> components) {
        if (components == null) {
            return;
        }

        for (Component component : components) {
            cleanupComponent(component);
            cleanupComponents(component.getComponents());
        }
    }

    protected static void addMissingSerialNumber(Bom bom) {
        SbomUtils.addMissingSerialNumber(bom);
    }

    /**
     * Adjusts the provided {@link Component} according to our standards.
     *
     * @param component
     */
    protected void cleanupComponent(Component component) {
        log.debug("No cleanup done for {}", this.getClass());
    }

    protected void cleanupExternalReferences(List<ExternalReference> externalReferences) {
        if (externalReferences != null) {
            externalReferences.removeIf(er -> !Strings.isEmpty(er.getUrl()) && !UriValidator.isUriValid(er.getUrl()));
        }
    }
}
