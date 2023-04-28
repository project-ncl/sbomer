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
package org.jboss.sbomer.cli.commands.processor;

import javax.inject.Inject;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.jboss.pnc.dto.Build;
import org.jboss.pnc.dto.BuildConfiguration;
import org.jboss.sbomer.cli.commands.processor.ProductVersionMapper.ProductVersionMapping;
import org.jboss.sbomer.cli.model.Sbom;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.utils.SbomUtils;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

/**
 * <p>
 * Processor to add the Red Hat Product information to the main {@link Component} within the {@link Bom}.
 * </p>
 */
@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "redhat-product",
        description = "Process the SBOM with Red Hat product enrichment")
public class RedHatProductProcessCommand extends AbstractProcessCommand {

    public final static String PROPERTY_ERRATA_PRODUCT_NAME = "errata-tool-product-name";
    public final static String PROPERTY_ERRATA_PRODUCT_VERSION = "errata-tool-product-version";
    public final static String PROPERTY_ERRATA_PRODUCT_VARIANT = "errata-tool-product-variant";

    @Inject
    ProductVersionMapper productVersionMapper;

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
            log.debug("Adding {} property with value: {}", property, value);
            SbomUtils.addProperty(component, property, value);
        } else {
            log.debug(
                    "Property {} already exist, value: {}",
                    property,
                    SbomUtils.findPropertyWithNameInComponent(property, component).get().getValue());
        }
    }

    @Override
    public ProcessorImplementation getImplementationType() {
        return ProcessorImplementation.REDHAT_PRODUCT;
    }

    @Override
    public Bom doProcess(Sbom sbom) {
        Bom bom = getBom(sbom);
        Build build = pncService.getBuild(sbom.getBuildId());

        if (build == null) {
            throw new ApplicationException(
                    "Build related to the SBOM could not be found in PNC, interrupting processing");
        }

        BuildConfiguration buildConfig = pncService.getBuildConfig(build.getBuildConfigRevision().getId());

        if (buildConfig == null) {
            throw new ApplicationException(
                    "BuildConfig related to the SBOM could not be found in PNC, interrupting processing");
        }

        if (buildConfig.getProductVersion() == null) {
            throw new ApplicationException(
                    "BuildConfig related to the SBOM does not provide product version information, interrupting processing");
        }

        ProductVersionMapping mapping = productVersionMapper.getMapping().get(buildConfig.getProductVersion().getId());

        if (mapping == null) {
            throw new ApplicationException(
                    "Could not find mapping for the PNC Product Version '{}' (id: {})",
                    buildConfig.getProductVersion().getVersion(),
                    buildConfig.getProductVersion().getId());
        }

        Component component = bom.getMetadata().getComponent();

        addPropertyIfMissing(component, PROPERTY_ERRATA_PRODUCT_NAME, mapping.getErrata().getProductName());
        addPropertyIfMissing(component, PROPERTY_ERRATA_PRODUCT_VERSION, mapping.getErrata().getProductVersion());
        addPropertyIfMissing(component, PROPERTY_ERRATA_PRODUCT_VARIANT, mapping.getErrata().getProductVariant());

        return bom;
    }
}
