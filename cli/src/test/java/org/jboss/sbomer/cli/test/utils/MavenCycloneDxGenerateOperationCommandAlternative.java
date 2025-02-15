package org.jboss.sbomer.cli.test.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import jakarta.enterprise.inject.Alternative;

import org.jboss.sbomer.cli.feature.sbom.command.CycloneDxGenerateOperationCommand;

import picocli.CommandLine.Command;

@Alternative
@Command(
        mixinStandardHelpOptions = true,
        name = "cyclonedx-operation",
        description = "SBOM generation for deliverable Maven POMs using the CycloneDX Maven plugin")
public class MavenCycloneDxGenerateOperationCommandAlternative extends CycloneDxGenerateOperationCommand {

    private static final String OPERATION_JSON = "boms/operation.json";

    @Override
    protected Path doGenerate() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(OPERATION_JSON)) {
            Objects.requireNonNull(in, "Resource " + OPERATION_JSON + " not found");
            Files.copy(in, getParent().getOutput());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return getParent().getOutput();
    }

}
