package org.jboss.sbomer.cli.test.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.inject.Alternative;

import org.jboss.sbomer.cli.feature.sbom.command.GradleCycloneDxGenerateCommand;
import org.jboss.sbomer.cli.feature.sbom.command.ProcessCommand;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Alternative
@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "gradle-cyclonedx-plugin",
        aliases = { "gradle-cyclonedx" },
        description = "SBOM generation for Gradle projects using the CycloneDX Gradle plugin",
        subcommands = { ProcessCommand.class })
public class GradleCycloneDxGenerateCommandMockAlternative extends GradleCycloneDxGenerateCommand {

    @Override
    protected void doClone(String url, String tag, Path path, boolean force) {
        log.info("Would clone url: {}, with tag: {}, into: {}, force: {}", url, tag, path, force);
    }

    @Override
    protected Path doGenerate(String buildCmdOptions) {

        try {
            Files.copy(getClass().getClassLoader().getResourceAsStream("boms/plain.json"), getParent().getOutput());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return getParent().getOutput();
    }

}
