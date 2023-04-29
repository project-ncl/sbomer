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
package org.jboss.sbomer.generator;

/**
 * SBOM generator interface.
 *
 * @author Marek Goldmann
 */
public interface SbomGenerator {

    /**
     * Generates the SBOM in CycloneDX format referenced by the SBOM sbomId.
     *
     * @param sbomId SBOM identifier.
     */
    default public void generate(Long sbomId) {
        generate(sbomId, null, null);
    }

    /**
     * Generates the SBOM in CycloneDX format referenced by the SBOM sbomId.
     *
     * @param sbomId SBOM identifier.
     * @param generatorVersion Generator version.
     */
    default public void generate(Long sbomId, String generatorVersion) {
        generate(sbomId, generatorVersion, null);
    }

    /**
     * Generates the SBOM in CycloneDX format referenced by the SBOM sbomId.
     *
     * @param sbomId SBOM identifier.
     * @param generatorVersion Generator version.
     * @param generatorArgs Generator arguments.
     */
    public void generate(Long sbomId, String generatorVersion, String generatorArgs);

}
