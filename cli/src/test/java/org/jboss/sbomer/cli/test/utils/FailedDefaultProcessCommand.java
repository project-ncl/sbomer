package org.jboss.sbomer.cli.test.utils;

import org.jboss.sbomer.cli.feature.sbom.command.DefaultProcessCommand;

import jakarta.enterprise.inject.Alternative;
import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine.Command;

@Alternative
@Slf4j
@Command(
        mixinStandardHelpOptions = true,
        name = "default",
        description = "Process the SBOM with enrichments applied to known CycloneDX fields")
public class FailedDefaultProcessCommand extends DefaultProcessCommand {

    @Override
    public Integer call() throws Exception {
        log.info("Mocking 'default' processor, failing the process");
        return 333;
    }
}
