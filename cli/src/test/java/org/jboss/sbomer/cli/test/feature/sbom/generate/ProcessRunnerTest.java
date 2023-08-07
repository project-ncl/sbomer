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
package org.jboss.sbomer.cli.test.feature.sbom.generate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jboss.sbomer.cli.feature.sbom.generate.ProcessRunner;
import org.jboss.sbomer.core.errors.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ProcessRunnerTest {

    @Test
    void noCommandProvided() {

        ValidationException thrown = Assertions.assertThrows(ValidationException.class, () -> {
            ProcessRunner.run(Path.of("some/dir"));
        });

        assertEquals("Command execution validation failed", thrown.getLocalizedMessage());
        assertEquals(1, thrown.getErrors().size());
        assertEquals("No command to provided", thrown.getErrors().get(0));
    }

    @Test
    void noWorkDirProvided() {

        ValidationException thrown = Assertions.assertThrows(ValidationException.class, () -> {
            ProcessRunner.run(null, "command");
        });

        assertEquals("Command execution validation failed", thrown.getLocalizedMessage());
        assertEquals(1, thrown.getErrors().size());
        assertEquals("No working directory provided", thrown.getErrors().get(0));
    }

    @Test
    void nonExistingWorkingDirectoryProvided() {
        ValidationException thrown = Assertions.assertThrows(ValidationException.class, () -> {
            ProcessRunner.run(Path.of("surely/doesnt/exist"), "command");
        });

        assertEquals("Command execution validation failed", thrown.getLocalizedMessage());
        assertEquals(1, thrown.getErrors().size());
        assertEquals("Provided working directory 'surely/doesnt/exist' does not exist", thrown.getErrors().get(0));
    }

    @Test
    void notAWorkingDirectoryDirectoryProvided(@TempDir Path tempDir) throws IOException {
        Path aFile = Files.createFile(Path.of(tempDir.toAbsolutePath().toString(), "a-file.txt"));

        ValidationException thrown = Assertions.assertThrows(ValidationException.class, () -> {
            ProcessRunner.run(aFile, "command");
        });

        assertEquals("Command execution validation failed", thrown.getLocalizedMessage());
        assertEquals(1, thrown.getErrors().size());
        assertEquals(
                String.format("Provided working directory '%s' is not a directory", aFile),
                thrown.getErrors().get(0));
    }
}
