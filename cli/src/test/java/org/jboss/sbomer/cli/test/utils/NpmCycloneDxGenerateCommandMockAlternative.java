package org.jboss.sbomer.cli.test.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.jboss.sbomer.cli.feature.sbom.command.NpmCycloneDxGenerateCommand;
import org.jboss.sbomer.cli.feature.sbom.command.ProcessCommand;

import jakarta.enterprise.inject.Alternative;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Alternative
@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "cyclonedx-npm",
        aliases = { "cyclonedx-npm-plugin" },
        description = "SBOM generation for Node.js NPM projects using the CycloneDX Nodejs plugin",
        subcommands = { ProcessCommand.class })
public class NpmCycloneDxGenerateCommandMockAlternative extends NpmCycloneDxGenerateCommand {

    @Override
    protected void doClone(String url, String tag, Path path, boolean force) {
        log.info("Would clone url: {}, with tag: {}, into: {}, force: {}", url, tag, path, force);
    }

    @Override
    protected Path doGenerate(String buildCmdOptions) {

        try {
            Files.copy(
                    Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("boms/plain.json")),
                    getParent().getOutput());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return getParent().getOutput();
    }

}
