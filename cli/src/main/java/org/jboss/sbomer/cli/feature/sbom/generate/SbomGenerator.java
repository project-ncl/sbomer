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
package org.jboss.sbomer.cli.feature.sbom.generate;

import java.nio.file.Path;

/**
 * An interface for the SBOM generator.
 *
 * @author Marek Goldmann
 */
public interface SbomGenerator {
    /**
     * Generates the SBOM for a project located at the given {@link Path}.
     *
     * @param dir The directory containing the source code to generate the SBOM for.
     * @return The {@link Path} to the generated SBOM.
     */
    public Path run(Path workDir, String... generatorArgs);

}
