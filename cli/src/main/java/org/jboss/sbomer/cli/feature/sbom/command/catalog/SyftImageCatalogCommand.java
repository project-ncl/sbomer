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
package org.jboss.sbomer.cli.feature.sbom.command.catalog;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Metadata;
import org.cyclonedx.model.Pedigree;
import org.cyclonedx.model.Property;
import org.cyclonedx.model.Variants;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import com.github.packageurl.MalformedPackageURLException;
import com.github.packageurl.PackageURL;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Command(
        mixinStandardHelpOptions = true,
        name = "image-index",
        description = "Creates the image index given a list of Syft manifests")
@Slf4j
public class SyftImageCatalogCommand extends AbstractCatalogCommand {

    @Override
    protected String getCataloguerType() {
        return "image-index";
    }

    @Override
    protected Bom doCatalog(String imageName, String imageDigest, List<Bom> boms) {
        log.debug(
                "Received {} manifests to be catalogued in an image index for {}:{}",
                boms.size(),
                imageName,
                imageDigest);

        if (boms.isEmpty()
                || boms.stream().anyMatch(bom -> bom.getComponents() == null || bom.getComponents().isEmpty())) {
            throw new ApplicationException("One or more BOMs lack components. Cannot proceed with cataloging.");
        }

        Variants indexVariants = new Variants();
        Bom indexBom = SbomUtils.createBom();
        // FIXME: This method can return null
        Objects.requireNonNull(indexBom);
        String indexPurl = SbomUtils.createContainerImageOCIPurl(imageName, imageDigest);
        String indexName = boms.get(0).getComponents().get(0).getName();
        String indexBaseBomPrefix = getBaseBomRefPrefix(boms.get(0).getComponents().get(0));

        // Process each BOM and add to variants
        for (Bom bom : boms) {
            Component componentVariant = createVariant(bom.getComponents().get(0), indexBaseBomPrefix);
            indexVariants.addComponent(componentVariant);
        }

        // Create the main component for the image index
        Component mainComponent = SbomUtils
                .createComponent(null, indexName, imageDigest, null, indexPurl, Component.Type.CONTAINER);
        mainComponent.setBomRef(indexBaseBomPrefix + "_image-index");

        Pedigree pedigree = new Pedigree();
        pedigree.setVariants(indexVariants);
        mainComponent.setPedigree(pedigree);

        // Let's retain the main properties in the index manifest
        copyProperties(mainComponent, boms.get(0).getComponents().get(0));

        setMetadataComponent(indexBom, mainComponent);

        indexBom.setComponents(List.of(mainComponent));

        SbomUtils.setPublisher(mainComponent);
        SbomUtils.setSupplier(mainComponent);
        SbomUtils.addMissingMetadataSupplier(indexBom);
        SbomUtils.addMissingSerialNumber(indexBom);
        SbomUtils.addMissingContainerHash(indexBom);

        return indexBom;
    }

    private void copyProperties(Component destination, Component origin) {
        List<String> propertyNames = List.of(
                "sbomer:image:labels:com.redhat.component",
                "sbomer:image:labels:name",
                "sbomer:image:labels:release",
                "sbomer:image:labels:version");

        propertyNames.forEach(
                propertyName -> SbomUtils.findPropertyWithNameInComponent(propertyName, origin)
                        .ifPresent(
                                property -> SbomUtils
                                        .addPropertyIfMissing(destination, property.getName(), property.getValue())));
    }

    private Component createVariant(Component mainComponent, String bomRefPrefix) {
        Component variant = SbomUtils.createComponent(mainComponent);
        String arch = getContainerArch(mainComponent);
        variant.setBomRef(bomRefPrefix + "_" + arch);

        SbomUtils.setPublisher(variant);
        SbomUtils.setSupplier(variant);

        return variant;
    }

    private void setMetadataComponent(Bom bom, Component mainComponent) {
        Component metadataComponent = new Component();
        metadataComponent.setType(mainComponent.getType());
        metadataComponent.setName(mainComponent.getName());
        metadataComponent.setPurl(mainComponent.getPurl());
        metadataComponent.setDescription("Image index manifest of " + mainComponent.getPurl());

        Metadata metadata = new Metadata();
        metadata.setComponent(metadataComponent);
        metadata.setTimestamp(Date.from(Instant.now()));
        metadata.setToolChoice(SbomUtils.createToolInformation(getSBOMerVersion()));
        bom.setMetadata(metadata);
    }

    private String getContainerArch(Component component) {
        try {
            PackageURL purl = new PackageURL(component.getPurl());
            Map<String, String> qualifiers = purl.getQualifiers();
            if (qualifiers != null && qualifiers.containsKey("arch")) {
                return qualifiers.get("arch");
            }
        } catch (MalformedPackageURLException e) {
            log.warn("Could not parse the PURL: {}", component.getPurl(), e);
        }

        return SbomUtils.findPropertyWithNameInComponent("sbomer:image:labels:architecture", component)
                .map(Property::getValue)
                .orElseThrow(
                        () -> new ApplicationException(
                                "The 'architecture' label was not found within the container image. Cannot proceed."));
    }

    private String getBaseBomRefPrefix(Component component) {
        return SbomUtils.findPropertyWithNameInComponent("sbomer:image:labels:com.redhat.component", component)
                .map(Property::getValue)
                .orElse(component.getName());
    }

}
