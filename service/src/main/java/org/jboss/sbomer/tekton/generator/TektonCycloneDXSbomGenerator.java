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
package org.jboss.sbomer.tekton.generator;

import static org.jboss.sbomer.core.enums.GeneratorImplementation.CYCLONEDX;

import javax.enterprise.context.ApplicationScoped;

import org.jboss.sbomer.generator.Generator;
import org.jboss.sbomer.generator.SbomGenerator;

/**
 * Implementation responsible for running the Maven CycloneDX generator.
 *
 * @author Marek Goldmann
 */
@Generator(CYCLONEDX)
@ApplicationScoped
public class TektonCycloneDXSbomGenerator extends AbstractGeneratorTektonTaskRunner implements SbomGenerator {

    @Override
    public void generate(Long sbomId, String buildId, String generatorVersion, String generatorArgs) {
        runTektonTask("sbomer-generate-cyclonedx", sbomId, buildId, CYCLONEDX, generatorVersion, generatorArgs);
    }
}
