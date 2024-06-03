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

import java.util.ArrayList;
import java.util.List;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Property;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.features.sbom.utils.UrlUtils;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

@ApplicationScoped
@Slf4j
public class SyftImageAdjuster implements Adjuster {
    @Override
    public Bom adjust(Bom bom) {
        Component productComponent = bom.getMetadata().getComponent();

        // Initialize properties for main component, if not done so yet
        if (productComponent.getProperties() == null) {
            productComponent.setProperties(new ArrayList<>());
        }

        // If there are properties in the metadata field of the manifest, move these into the main component's
        // properties.
        if (bom.getMetadata().getProperties() != null) {
            log.debug(
                    "Moving '{}' properties from metadata to main component",
                    bom.getMetadata().getProperties().size());

            productComponent.getProperties().addAll(bom.getMetadata().getProperties());
            bom.getMetadata().setProperties(null);
        }

        // Cleanup main component
        cleanupComponent(productComponent);

        // ...and all other components
        bom.getComponents().forEach(component -> {
            cleanupComponent(component);
        });

        // Set the purl for the main component
        productComponent.setPurl(
                new StringBuilder("pkg:oci/").append(productComponent.getName().split("/")[2])
                        .append("@")
                        .append(UrlUtils.urlencode(productComponent.getVersion()))
                        .toString());

        // Remove all components that are not on the paths we are interested in
        bom.getComponents().removeIf(c -> {
            return c.getProperties()
                    .stream()
                    // TODO: this is hardocded and needs to be customizable
                    .filter(p -> p.getName().equals("sbomer:location:0:path") && p.getValue().startsWith("/opt"))
                    .findAny()
                    .isEmpty();
        });

        // Populate dependencies section with components
        populateDependencies(bom);

        return bom;
    }

    private void populateDependencies(Bom bom) {
        List<Dependency> dependencies = new ArrayList<>();

        Dependency product = SbomUtils.createDependency(bom.getMetadata().getComponent().getPurl());

        dependencies.add(product);

        // For each component, if it's not already in the dependencies, add it to both: dependency list as well as a
        // dependency to the main product dependency
        bom.getComponents().forEach(c -> {
            if (dependencies.stream().anyMatch(d -> d.getRef().equals(c.getPurl()))) {
                return;
            }

            Dependency dependency = SbomUtils.createDependency(c.getPurl());
            dependencies.add(dependency);
            product.addDependency(dependency);

        });

        bom.setDependencies(dependencies);
    }

    private void cleanupComponent(Component component) {
        log.debug("Cleaning up component '{}'", component.getPurl());

        // Remove CPE, we don't use it now
        component.setCpe(null);
        // Remove BOM ref, we don't use it now
        component.setBomRef(null);

        if (component.getProperties() != null) {
            List<String> supportedPropsPrefixes = List.of(
                    "sbomer:package:language",
                    "sbomer:package:type",
                    "sbomer:location:0:path",
                    "sbomer:image:labels");

            // Adjust property names
            component.getProperties().forEach(p -> {
                String newName = p.getName().replace("syft:", "sbomer:");

                log.debug("Adjusting property name from '{}' to '{}'", p.getName(), newName);

                p.setName(newName);
            });

            // Remove properties we don't care about
            component.getProperties().removeIf(prop -> {
                boolean supportedProp = supportedPropsPrefixes.stream()
                        .anyMatch(prefix -> prop.getName().startsWith(prefix));

                if (!supportedProp) {
                    log.debug(
                            "Property '{}' with value '{}' is not on the supported properties list, removing...",
                            prop.getName(),
                            prop.getValue());
                }

                return !supportedProp;
            });

            // Adjust purl for Java components
            Property propType = SbomUtils.findPropertyWithNameInComponent("sbomer:package:type", component)
                    .orElse(null);

            if (component.getPurl() != null && propType != null && propType.getValue().equals("java-archive")) {
                log.debug(
                        "Adjusting purl for the '{}' Java component by adding '?type=jar' suffix...",
                        component.getPurl());
                component.setPurl(component.getPurl() + "?type=jar");
            }
        }

        log.debug("Component '{}' adjusted", component.getPurl());
    }
}
