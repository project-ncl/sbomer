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
package org.jboss.sbomer.service.nextgen.core.generator;

import java.util.Set;

import org.jboss.sbomer.service.nextgen.core.dto.model.GenerationRecord;

public interface Generator {
    /**
     * A set of supported target deliverable types.
     */
    public Set<String> getSupportedTypes();

    /**
     * <p>
     * Returns a name of the generator which is its identifier at the same time.
     * </p>
     *
     * <p>
     * It should contain only lower case letters and optionally '-' sign.
     * </p>
     *
     * @return Generator name
     */
    public String getGeneratorName();

    /**
     * Returns a version of the generator.
     *
     * @return Generator version
     */
    public String getGeneratorVersion();

    /**
     * Main method that initiates generation.
     *
     * @param generationRecord
     */
    public void generate(GenerationRecord generationRecord);
}
