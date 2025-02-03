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
package org.jboss.sbomer.service.feature.sbom.config.features;

import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_NAME;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_VARIANT;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_VERSION;

import java.util.Optional;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.model.Property;
import org.jboss.sbomer.core.features.sbom.utils.SbomUtils;

import lombok.Builder;
import lombok.Data;

/**
 * <p>
 * Product pointer to which the Sbom is related.
 * </p>
 *
 *
 * <p>
 * This object can contain one or more entries that point to a product configuration. These can use different sources
 * for this information.
 * </p>
 */
@Data
@Builder
public class ProductConfig {
    @Data
    @Builder
    public static class ErrataProductConfig {
        String productName;
        String productVersion;
        String productVariant;

        /**
         * <p>
         * Generates the {@link ErrataProductConfig} object based on the data available in the CycloneDX {@link Bom}
         * for the main component.
         * </p>
         *
         * <p>
         * In case required properties cannot be found, {@code null} is returned.
         * </p>
         *
         * @param bom The {@link Bom} BOM to be used for retrieving the Product config
         * @return The {@link ErrataProductConfig} object or {@code null} if data cannot be found.
         */
        public static ErrataProductConfig fromBom(Bom bom) {
            Component component = bom.getMetadata().getComponent();

            Optional<Property> productName = SbomUtils
                    .findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_NAME, component);
            Optional<Property> productVersion = SbomUtils
                    .findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_VERSION, component);
            Optional<Property> productVariant = SbomUtils
                    .findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_VARIANT, component);

            if (productName.isEmpty() || productVersion.isEmpty() || productVariant.isEmpty()) {
                return null;
            }

            return ErrataProductConfig.builder()
                    .productName(productName.get().getValue())
                    .productVersion(productVersion.get().getValue())
                    .productVariant(productVariant.get().getValue())
                    .build();
        }
    }

    /**
     * Product information stored in the Errata Tool.
     */
    ErrataProductConfig errataTool;
}
