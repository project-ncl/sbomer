package org.jboss.sbomer.cli.test.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.inject.Alternative;

import org.jboss.sbomer.cli.feature.sbom.command.CycloneDxGenerateOperationCommand;

import picocli.CommandLine.Command;

@Alternative
@Command(
        mixinStandardHelpOptions = true,
        name = "cyclonedx-operation",
        description = "SBOM generation for deliverable Maven POMs using the CycloneDX Maven plugin")
public class MavenCycloneDxGenerateOperationCommandAlternative extends CycloneDxGenerateOperationCommand {

    @Override
    protected Path doGenerate() {

        try {
            Files.copy(getClass().getClassLoader().getResourceAsStream("boms/operation.json"), getParent().getOutput());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return getParent().getOutput();
    }

}
