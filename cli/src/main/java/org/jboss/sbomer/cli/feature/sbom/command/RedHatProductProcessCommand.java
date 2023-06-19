/**
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

import static org.jboss.sbomer.feature.sbom.core.Constants.PROPERTY_ERRATA_PRODUCT_NAME;
import static org.jboss.sbomer.feature.sbom.core.Constants.PROPERTY_ERRATA_PRODUCT_VARIANT;
import static org.jboss.sbomer.feature.sbom.core.Constants.PROPERTY_ERRATA_PRODUCT_VERSION;

import javax.inject.Inject;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.jboss.sbomer.core.service.PncService;
import org.jboss.sbomer.feature.sbom.core.enums.ProcessorType;
import org.jboss.sbomer.feature.sbom.core.utils.SbomUtils;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "redhat-product",
        description = "Process the SBOM with Red Hat product enrichment")
public class RedHatProductProcessCommand extends AbstractProcessCommand {

    @Option(names = { "--productName" }, description = "Product name in Errata Tool.", required = true)
    String productName;

    @Option(names = { "--productVersion" }, description = "Product version in Errata Tool.", required = true)
    String productVersion;

    @Option(names = { "--productVariant" }, description = "Product variant in Errata Tool.", required = true)
    String productVariant;

    @ParentCommand
    ProcessCommand parent;

    @Inject
    protected PncService pncService;

    /**
     * <p>
     * Adds a property of a given name in case it's not already there.
     * </p>
     *
     * @param component The {@link Component} to add the property to.
     * @param property The name of the property.
     * @param value The value of the property.
     */
    private void addPropertyIfMissing(Component component, String property, String value) {
        if (!SbomUtils.hasProperty(component, property)) {
            log.info("Adding {} property with value: {}", property, value);
            SbomUtils.addProperty(component, property, value);
        } else {
            log.debug(
                    "Property {} already exist, value: {}",
                    property,
                    SbomUtils.findPropertyWithNameInComponent(property, component).get().getValue());
        }
    }

    @Override
    public ProcessorType getImplementationType() {
        return ProcessorType.REDHAT_PRODUCT;
    }

    @Override
    public Bom doProcess(Bom bom) {
        Component component = bom.getMetadata().getComponent();

        addPropertyIfMissing(component, PROPERTY_ERRATA_PRODUCT_NAME, productName);
        addPropertyIfMissing(component, PROPERTY_ERRATA_PRODUCT_VERSION, productVersion);
        addPropertyIfMissing(component, PROPERTY_ERRATA_PRODUCT_VARIANT, productVariant);

        return bom;
    }
}
