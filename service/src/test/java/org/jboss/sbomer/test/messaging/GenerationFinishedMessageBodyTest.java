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
package org.jboss.sbomer.test.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.messaging.umb.Build;
import org.jboss.sbomer.messaging.umb.Build.BuildSystem;
import org.jboss.sbomer.messaging.umb.GenerationFinishedMessageBody;
import org.jboss.sbomer.messaging.umb.GenerationFinishedMessageBodyValidator;
import org.jboss.sbomer.messaging.umb.GenerationFinishedMessageBodyValidator.ValidationResult;
import org.jboss.sbomer.messaging.umb.ProductConfig;
import org.jboss.sbomer.messaging.umb.ProductConfig.ErrataProductConfig;
import org.jboss.sbomer.messaging.umb.Sbom;
import org.jboss.sbomer.messaging.umb.Sbom.Bom;
import org.jboss.sbomer.messaging.umb.Sbom.BomFormat;
import org.junit.jupiter.api.Test;

public class GenerationFinishedMessageBodyTest {

    private GenerationFinishedMessageBody genValidMessageBody() {
        return GenerationFinishedMessageBody.builder()
                .purl("as")
                .productConfig(
                        ProductConfig.builder()
                                .errataTool(
                                        ErrataProductConfig.builder()
                                                .productName("BLAH")
                                                .productVersion("AABB")
                                                .productVariant("DDDD")
                                                .build())
                                .build())
                .sbom(
                        Sbom.builder()
                                .id("429305915731435500")
                                .link("https://sbomer/api/v1alpha1/sboms/429305915731435500")
                                .bom(
                                        Bom.builder()
                                                .format(BomFormat.CYCLONEDX)
                                                .version("1.4")
                                                .link("https://sbomer/api/v1alpha1/sboms/429305915731435500/bom")
                                                .build())
                                .build())
                .build(
                        Build.builder()
                                .id("AWY5AHXBLAQAA")
                                .buildSystem(BuildSystem.PNC)
                                .link("https://orch.psi.redhat.com/pnc-rest/v2/builds/AWY5AHXBLAQAA")
                                .build())
                .build();
    }

    @Test
    void testValidityOfEmptyObject() {
        GenerationFinishedMessageBody message = GenerationFinishedMessageBody.builder().build();
        ValidationResult result = GenerationFinishedMessageBodyValidator.validate(message);

        assertEquals(4, result.getErrors().size());
        assertFalse(result.isValid());

        assertThat(
                result.getErrors(),
                CoreMatchers.hasItems(
                        "#: Instance does not have required property \"purl\"",
                        "#: Instance does not have required property \"productConfig\"",
                        "#: Instance does not have required property \"sbom\"",
                        "#: Instance does not have required property \"build\""));

    }

    @Test
    void testJustWithPurl() {
        GenerationFinishedMessageBody message = GenerationFinishedMessageBody.builder().purl("as").build();
        ValidationResult result = GenerationFinishedMessageBodyValidator.validate(message);

        assertEquals(3, result.getErrors().size());
        assertFalse(result.isValid());

        assertThat(
                result.getErrors(),
                CoreMatchers.hasItems(
                        "#: Instance does not have required property \"productConfig\"",
                        "#: Instance does not have required property \"sbom\"",
                        "#: Instance does not have required property \"build\""));
    }

    @Test
    void testValid() {
        GenerationFinishedMessageBody message = genValidMessageBody();
        ValidationResult result = GenerationFinishedMessageBodyValidator.validate(message);

        assertEquals(Collections.emptyList(), result.getErrors());
        assertTrue(result.isValid());
    }

    @Test
    void testPartialErrataPoductConfig() {
        GenerationFinishedMessageBody message = genValidMessageBody();
        message.getProductConfig().getErrataTool().setProductVersion(null);

        ValidationResult result = GenerationFinishedMessageBodyValidator.validate(message);

        assertEquals(3, result.getErrors().size());
        assertFalse(result.isValid());

        assertThat(
                result.getErrors(),
                CoreMatchers.hasItems(
                        "#: Property \"productConfig\" does not match schema",
                        "#/productConfig: Property \"errataTool\" does not match schema",
                        "#/productConfig/errataTool: Instance does not have required property \"productVersion\""));
    }

    @Test
    void testSbomMissingLink() {
        GenerationFinishedMessageBody message = genValidMessageBody();
        message.getSbom().setLink(null);

        ValidationResult result = GenerationFinishedMessageBodyValidator.validate(message);

        assertEquals(2, result.getErrors().size());
        assertFalse(result.isValid());

        assertThat(
                result.getErrors(),

                CoreMatchers.hasItems(
                        "#: Property \"sbom\" does not match schema",
                        "#/sbom: Instance does not have required property \"link\""));
    }
}
