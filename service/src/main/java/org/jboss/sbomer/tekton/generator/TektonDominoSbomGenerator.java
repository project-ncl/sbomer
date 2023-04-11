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

import static org.jboss.sbomer.core.enums.GeneratorImplementation.DOMINO;

import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.sbomer.core.errors.ApplicationException;
import org.jboss.sbomer.generator.Generator;
import org.jboss.sbomer.generator.SbomGenerator;
import org.jboss.sbomer.tekton.AbstractTektonTaskRunner;

/**
 * Implementation responsible for running the Domino SBOM generator.
 */
@Generator(DOMINO)
@ApplicationScoped
public class TektonDominoSbomGenerator extends AbstractTektonTaskRunner implements SbomGenerator {

    @ConfigProperty(name = "sbomer.domino.default-version")
    String dominoDefaultVersion;

    @ConfigProperty(name = "sbomer.domino.additional-args")
    String dominoAdditionalArgs;

    @Override
    public void generate(String buildId) throws ApplicationException {
        var config = Json.createObjectBuilder()
                .add("version", dominoDefaultVersion)
                .add("additional-args", dominoAdditionalArgs)
                .build();
        runTektonTask("sbomer-generate-domino", buildId, config);
    }

}
