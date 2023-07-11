
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
package org.jboss.sbomer.service.feature.sbom.features.umb.producer.model;

import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.core.features.sbom.utils.ObjectMapperProvider;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.Builder;
import lombok.Data;

/**
 * <p>
 * Class representing the body of the UMB message sent after the generation is finished.
 * </p>
 *
 * <p>
 * It is validated according to the available in the {@code message-success-schema.json} file.
 * </p>
 *
 */
@Data
@Builder
public class GenerationFinishedMessageBody {

    /**
     * The package URL to uniquely identify the main component for which the SBOM was generated.
     */
    String purl;

    /**
     * Product information. for which the main component is
     */
    ProductConfig productConfig;

    /**
     * Sbom details.
     */
    Sbom sbom;

    /**
     * Build details.
     */
    Build build;

    /**
     * Convenience method to convert the message into a JSON {@link String}.
     *
     * @return A stringified JSON representation of the message.
     */
    @JsonIgnore
    public String toJson() {
        try {
            return ObjectMapperProvider.json().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new ApplicationException("Could not serialize message body", e);
        }
    }

}
