package org.jboss.sbomer.cli.test.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import jakarta.enterprise.inject.Alternative;

import org.jboss.sbomer.cli.feature.sbom.command.MavenCycloneDxGenerateCommand;
import org.jboss.sbomer.cli.feature.sbom.command.ProcessCommand;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Alternative
@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "maven-cyclonedx-plugin",
        aliases = { "maven-cyclonedx" },
        description = "SBOM generation for Maven projects using the CycloneDX Maven plugin",
        subcommands = { ProcessCommand.class })
public class MavenCycloneDxGenerateCommandMockAlternative extends MavenCycloneDxGenerateCommand {

    private static final String PLAIN_JSON = "boms/plain.json";

    @Override
    protected void doClone(String url, String tag, Path path, boolean force) {
        log.info("Would clone url: {}, with tag: {}, into: {}, force: {}", url, tag, path, force);
    }

    @Override
    protected Path doGenerate(String buildCmdOptions) {

        try (InputStream in = getClass().getClassLoader().getResourceAsStream(PLAIN_JSON)) {
            Objects.requireNonNull(in, "Resource " + PLAIN_JSON + " not found");
            Files.copy(in, getParent().getOutput());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return getParent().getOutput();
    }

}
