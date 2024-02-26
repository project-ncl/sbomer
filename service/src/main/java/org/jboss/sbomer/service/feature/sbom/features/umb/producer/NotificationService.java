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
import static org.jboss.sbomer.core.features.sbom.Constants.SBOM_RED_HAT_PNC_OPERATION_ID;
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
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.Operation;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.Build;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.Build.BuildSystem;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.GenerationFinishedMessageBody;
import org.jboss.sbomer.service.feature.sbom.features.umb.producer.model.OperationGenerationFinishedMessageBody;
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

    @Inject
    OperationGenerationFinishedMessageBodyValidator operationValidator;

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

        if (sboms == null || sboms.isEmpty()) {
            log.info("No SBOMs provided to send notifications for");
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

            /**
             * https://issues.redhat.com/browse/SBOMER-19
             *
             * Skips sending UMB messages for manifests not related to a product build.
             */
            if (getProductConfiguration(bom) == null) {
                log.info(
                        "Could not retrieve product configuration from the main component (purl = '{}') in the '{}' SBOM, skipping sending UMB notification",
                        component.getPurl(),
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

    public void notifyOperationCompleted(List<org.jboss.sbomer.service.feature.sbom.model.Sbom> sboms) {

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

        if (sboms == null || sboms.isEmpty()) {
            log.info("No SBOMs provided to send notifications for");
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

            /**
             * https://issues.redhat.com/browse/SBOMER-19
             *
             * Skips sending UMB messages for manifests not related to a product build.
             */
            if (getProductConfiguration(bom) == null) {
                log.info(
                        "Could not retrieve product configuration from the main component (purl = '{}') in the '{}' SBOM, skipping sending UMB notification",
                        component.getPurl(),
                        sbom.getId());
                return;
            }

            OperationGenerationFinishedMessageBody msg = createOperationGenerationFinishedMessage(sbom, bom);

            ValidationResult result = operationValidator.validate(msg);
            if (result.isValid()) {
                log.info("OperationGenerationFinishedMessage is valid, sending it to the topic!");

                amqpMessageProducer.notify(msg);
            } else {
                log.warn(
                        "OperationGenerationFinishedMessage is NOT valid, NOT sending it to the topic! Validation errors: {}",
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
                .id(sbom.getIdentifier())
                .buildSystem(pncBuildSystemRef.isPresent() ? BuildSystem.PNC : null)
                .link(pncBuildSystemRef.isPresent() ? pncBuildSystemRef.get().getUrl() : null)
                .build();

        ProductConfig.ErrataProductConfig errataProductConfigPayload = getProductConfiguration(bom);
        ProductConfig productConfigPayload = ProductConfig.builder().errataTool(errataProductConfigPayload).build();

        return GenerationFinishedMessageBody.builder()
                .purl(sbom.getRootPurl())
                .sbom(sbomPayload)
                .build(buildPayload)
                .productConfig(productConfigPayload)
                .build();
    }

    private OperationGenerationFinishedMessageBody createOperationGenerationFinishedMessage(
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

        Optional<ExternalReference> pncOperationRef = getExternalReferences(
                component,
                ExternalReference.Type.BUILD_SYSTEM).stream()
                .filter(r -> r.getComment().equals(SBOM_RED_HAT_PNC_OPERATION_ID))
                .findFirst();

        Operation operationPayload = Operation.builder()
                .id(sbom.getIdentifier())
                .buildSystem(pncOperationRef.isPresent() ? Operation.BuildSystem.PNC : null)
                .link(pncOperationRef.isPresent() ? pncOperationRef.get().getUrl() : null)
                .deliverable(component.getVersion())
                .build();

        ProductConfig.ErrataProductConfig errataProductConfigPayload = getProductConfiguration(bom);
        ProductConfig productConfigPayload = ProductConfig.builder().errataTool(errataProductConfigPayload).build();

        return OperationGenerationFinishedMessageBody.builder()
                .purl(sbom.getRootPurl())
                .sbom(sbomPayload)
                .operation(operationPayload)
                .productConfig(productConfigPayload)
                .build();
    }

    /**
     * <p>
     * Generates the {@link ProductConfig.ErrataProductConfig} object based on the data available in the the CycloneDX
     * {@link org.cyclonedx.model.Bom} for the main component.
     * </p>
     *
     * <p>
     * In case required properties cannot be found, {@code null} is returned.
     * </p>
     *
     * @param bom The {@link org.cyclonedx.model.Bom} BOM to be used for retrieving the Product config
     * @return The {@link ProductConfig.ErrataProductConfig} object or {@code null} if data cannot be found.
     */
    private ProductConfig.ErrataProductConfig getProductConfiguration(org.cyclonedx.model.Bom bom) {
        Component component = bom.getMetadata().getComponent();
        Optional<Property> productName = findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_NAME, component);
        Optional<Property> productVersion = findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_VERSION, component);
        Optional<Property> productVariant = findPropertyWithNameInComponent(PROPERTY_ERRATA_PRODUCT_VARIANT, component);

        if (productName.isEmpty() || productVersion.isEmpty() || productVariant.isEmpty()) {
            return null;
        }

        return ProductConfig.ErrataProductConfig.builder()
                .productName(productName.get().getValue())
                .productVersion(productVersion.get().getValue())
                .productVariant(productVariant.get().getValue())
                .build();
    }
}