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
package org.jboss.sbomer.cli.feature.sbom.processor;

import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_NAME;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_VARIANT;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_VERSION;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.addPropertyIfMissing;

import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.jboss.sbomer.core.features.sbom.enums.ProcessorType;

public class RedHatProductProcessor implements Processor {

    protected final String productName;

    protected final String productVersion;

    protected final String productVariant;

    public RedHatProductProcessor(String productName, String productVersion, String productVariant) {
        this.productName = productName;
        this.productVersion = productVersion;
        this.productVariant = productVariant;
    }

    @Override
    public ProcessorType getType() {
        return ProcessorType.REDHAT_PRODUCT;
    }

    @Override
    public Bom process(Bom bom) {
        Component component = bom.getComponents().get(0);

        addPropertyIfMissing(component, PROPERTY_ERRATA_PRODUCT_NAME, productName);
        addPropertyIfMissing(component, PROPERTY_ERRATA_PRODUCT_VERSION, productVersion);
        addPropertyIfMissing(component, PROPERTY_ERRATA_PRODUCT_VARIANT, productVariant);

        return bom;
    }

}
