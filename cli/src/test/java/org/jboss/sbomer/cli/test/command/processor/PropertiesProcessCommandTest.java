package org.jboss.sbomer.cli.test.command.processor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.inject.Inject;

import org.jboss.sbomer.cli.CLI;
import org.jboss.sbomer.cli.commands.processor.PropertiesProcessCommand;
import org.jboss.sbomer.core.enums.ProcessorImplementation;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PropertiesProcessCommandTest extends PropertiesProcessCommand {
    @Inject
    CLI cli;

    @Test
    void shouldReturnCorrectImplementationType() {
        assertEquals(ProcessorImplementation.PROPERTIES, this.getImplementationType());
    }

    @Test
    void shouldManipulateProperties() {
    }
}
