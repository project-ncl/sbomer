package org.jboss.sbomer.cli.commands;

import javax.inject.Inject;

import org.jboss.sbomer.cli.CLI;

import picocli.CommandLine.Command;

// TODO: Add paramaters and implement the generation
@Command(mixinStandardHelpOptions = true, name = "generate", aliases = { "g" }, description = "Generate SBOM")
public class GenerateCommand implements Runnable {

    @Inject
    CLI cli;

    @Override
    public void run() {

        cli.usage(this.getClass());
    }

}
