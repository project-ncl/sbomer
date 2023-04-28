
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
package org.jboss.sbomer.features.umb.producer.model;

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
    }

    /**
     * Product information stored in the Errata Tool.
     */
    ErrataProductConfig errataTool;
}
