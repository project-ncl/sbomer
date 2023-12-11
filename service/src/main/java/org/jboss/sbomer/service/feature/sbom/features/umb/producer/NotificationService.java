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
package org.jboss.sbomer.service.feature.sbom.features.umb.producer;

import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_NAME;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_VARIANT;
import static org.jboss.sbomer.core.features.sbom.Constants.PROPERTY_ERRATA_PRODUCT_VERSION;
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_PNC_BUILD_ID;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.findPropertyWithNameInComponent;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.fromJsonNode;
import static org.jboss.sbomer.core.features.sbom.utils.SbomUtils.getExternalReferences;

import java.util.List;
import java.util.Optional;

import org.cyclonedx.model.Component;
import org.cyclonedx.model.ExternalReference;
import org.cyclonedx.model.Property;
import org.jboss.sbomer.core.SchemaValidator.ValidationResult;
import org.jboss.sbomer.service.feature.sbom.config.SbomerConfig;
import org.jboss.sbomer.service.feature.sbom.config.features.ProductConfig;
import org.jboss.sbomer.service.feature.sbom.config.features.UmbConfig;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.Build;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.Build.BuildSystem;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.GenerationFinishedMessageBody;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.Sbom;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.Sbom.BomFormat;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.Sbom.GenerationRequest;
import org.jboss.sbomer.service.feature.sbom.service.SbomRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation responsible for the notification of completed SBOMs.
 *
 * @author Andrea Vibelli
 */
@ApplicationScoped
@Slf4j
public class NotificationService {

    @Inject
    SbomerConfig sbomerConfig;

    @Inject
    SbomRepository sbomRepository;

    @Inject
    UmbConfig umbConfig;

    @Inject
    AmqpMessageProducer amqpMessageProducer;

    @Inject
    GenerationFinishedMessageBodyValidator validator;

    public void notifyCompleted(List<org.jboss.sbomer.service.feature.sbom.model.Sbom> sboms) {

        if (!umbConfig.isEnabled()) {
            log.info("UMB feature disabled, notification service won't send a notification");
            return;
        }

        if (!umbConfig.producer().isEnabled()) {
            log.info("UMB feature to produce notification messages disabled");
            return;
        }

        if (!umbConfig.producer().topic().isPresent()) {
            log.info("UMB produce topic not specified");
            return;
        }

        sboms.forEach(sbom -> {
            org.cyclonedx.model.Bom bom = fromJsonNode(sbom.getSbom());

            if (bom == null) {
                log.warn(
                        "Could not find a valid bom for SBOM id '{}', skipping sending UMB notification",
                        sbom.getId());
                return;
            }

            Component component = bom.getMetadata().getComponent();

            if (component == null) {
                log.warn(
                        "Could not find root metadata component for SBOM id '{}', skipping sending UMB notification",
                        sbom.getId());
                return;
            }

            GenerationFinishedMessageBody msg = createGenerationFinishedMessage(sbom, bom);

            ValidationResult result = validator.validate(msg);
            if (result.isValid()) {
                log.info("GenerationFinishedMessage is valid, sending it to the topic!");

                amqpMessageProducer.notify(msg);
            } else {
                log.warn(
                        "GenerationFinishedMessage is NOT valid, NOT sending it to the topic! Validation errors: {}",
                        String.join("; ", result.getErrors()));
            }
        });

    }

    private GenerationFinishedMessageBody createGenerationFinishedMessage(
            org.jboss.sbomer.service.feature.sbom.model.Sbom sbom,
            org.cyclonedx.model.Bom bom) {

        Component component = bom.getMetadata().getComponent();
        BomFormat bomFormat = null;

        try {
            bomFormat = BomFormat.valueOf(bom.getBomFormat().toUpperCase());
        } catch (IllegalArgumentException exc) {
            log.warn(
                    "Could not find compatible bom format for SBOM id '{}', found '{}', skipping sending UMB notification",
                    sbom.getId(),
                    bom.getBomFormat());
        }

        Sbom.Bom bomPayload = Sbom.Bom.builder()
                .format(bomFormat)
                .version(bom.getSpecVersion())
                .link(sbomerConfig.apiUrl() + "sboms/" + sbom.getId() + "/bom")
                .build();

        Sbom sbomPayload = Sbom.builder()
                .id(String.valueOf(sbom.getId()))
                .link(sbomerConfig.apiUrl() + "sboms/" + sbom.getId())
                .generationRequest(GenerationRequest.builder().id(sbom.getGenerationRequest().getId()).build())
                .bom(bomPayload)
                .build();

        Optional<ExternalReference> pncBuildSystemRef = getExternalReferences(
                component,
                ExternalReference.Type.BUILD_SYSTEM).stream()
                        .filter(r -> r.getComment().equals(SBOM_RED_HAT_PNC_BUILD_ID))
                        .findFirst();

        Build buildPayload = Build.builder()
                .id(sbom.getBuildId())
                .buildSystem(pncBuildSystemRef.isPresent() ? BuildSystem.PNC : null)
                .link(pncBuildSystemRef.isPresent() ? pncBuildSystemRef.get().getUrl() : null)
                .build();

        Optional<Property> productName = findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_NAME, component);
        Optional<Property> productVersion = findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_VERSION, component);
        Optional<Property> productVariant = findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_VARIANT, component);

        ProductConfig.ErrataProductConfig errataProductConfigPayload = ProductConfig.ErrataProductConfig.builder()
                .productName(productName.isPresent() ? productName.get().getValue() : null)
                .productVersion(productVersion.isPresent() ? productVersion.get().getValue() : null)
                .productVariant(productVariant.isPresent() ? productVariant.get().getValue() : null)
                .build();
        ProductConfig productConfigPayload = ProductConfig.builder().errataTool(errataProductConfigPayload).build();

        return GenerationFinishedMessageBody.builder()
                .purl(sbom.getRootPurl())
                .sbom(sbomPayload)
                .build(buildPayload)
                .productConfig(productConfigPayload)
                .build();
    }
}