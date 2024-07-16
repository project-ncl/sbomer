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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Dependency;
import org.cyclonedx.model.Property;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;
import org.jboss.sbomer.core.features.sbom.utils.UrlUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SyftImageAdjuster implements Adjuster {
    List<String> paths;
    boolean includeRpms;

    public SyftImageAdjuster(List<String> paths, boolean includeRpms) {
        this.paths = paths;
        this.includeRpms = includeRpms;
    }

    private boolean isOnPath(String path) {
        // In case we haven't provided paths to filter, add all found artifacts.
        if (paths == null || paths.isEmpty()) {
            return true;
        }

        return paths.stream().anyMatch(prefix -> path.startsWith(prefix));
    }

    @Override
    public Bom adjust(Bom bom, Path workDir) {
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

        // Remove components from manifest according to 'paths' and 'includeRpms' parameters
        bom.getComponents().removeIf(c -> {

            // Handle RPMs
            if (c.getPurl() != null && c.getPurl().startsWith("pkg:rpm")) {
                // Remove all components that are RPMs if the includeRpms is not set to true
                return !includeRpms;
            } else {
                // Handle everything else

                // If paths are not specified, include everything
                if (paths == null || paths.isEmpty()) {
                    return false;
                }

                // Remove all components that are not on the paths we are interested in
                return c.getProperties()
                        .stream()
                        .filter(p -> p.getName().equals("syft:location:0:path") && isOnPath(p.getValue()))
                        .findAny()
                        .isEmpty();
            }
        });

        // Cleanup main component
        cleanupComponent(productComponent);

        // ...and all other components
        bom.getComponents().forEach(this::cleanupComponent);

        // Set the purl for the main component
        productComponent.setPurl(generateImagePurl(productComponent, workDir));

        // Populate dependencies section with components
        populateDependencies(bom);

        return bom;
    }

    private String generateImagePurl(Component component, Path workDir) {
        String tag = null;
        ContainerImageInspectOutput inspectData = null;

        try {
            inspectData = ObjectMapperProvider.json()
                    .readValue(
                            Path.of(workDir.toAbsolutePath().toString(), "skopeo.json").toFile(),
                            ContainerImageInspectOutput.class);

        } catch (IOException e) {
            throw new ApplicationException("Could not read 'skopeo inpect' output", e);
        }

        // 7.4.17
        Optional<Property> versionOpt = SbomUtils
                .findPropertyWithNameInComponent("sbomer:image:labels:version", component);

        if (versionOpt.isEmpty()) {
            throw new ApplicationException(
                    "The 'version' label was not found within the container image, cannot proceed");
        }

        // 5.1717585311
        Optional<Property> releaseOpt = SbomUtils
                .findPropertyWithNameInComponent("sbomer:image:labels:release", component);

        if (releaseOpt.isEmpty()) {
            throw new ApplicationException(
                    "The 'release' label was not found within the container image, cannot proceed");
        }

        tag = versionOpt.get().getValue() + "-" + releaseOpt.get().getValue();

        // Split the name, if required
        String[] componentNameParts = component.getName().split("/");

        StringBuilder builder = new StringBuilder("pkg:oci/") //
                .append(componentNameParts[componentNameParts.length - 1])
                .append("@")
                .append(UrlUtils.urlencode(inspectData.getDigest()))
                .append("?")
                .append("repository_url=")
                .append(componentNameParts[0]) // This should be the registry the image was pulled from
                .append("&")
                .append("os=")
                .append(inspectData.getOs())
                .append("&")
                .append("arch=")
                .append(inspectData.getArchitecture())
                .append("&")
                .append("tag=")
                .append(tag);

        String purl = builder.toString();

        log.debug("Generated purl: '{}'", purl);

        return purl;
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
