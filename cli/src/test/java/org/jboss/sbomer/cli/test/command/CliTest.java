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
package org.jboss.sbomer.cli.test.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;

import org.hamcrest.CoreMatchers;
import org.jboss.sbomer.cli.CLI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class CliTest {
    @Inject
    CLI cli;

    StringWriter out = new StringWriter();
    StringWriter err = new StringWriter();

    @BeforeEach
    void init() {
        out = new StringWriter();
        err = new StringWriter();
    }

    @AfterEach
    void cleanup() throws IOException {
        out.close();
        err.close();
    }

    @Test
    void shouldPrintUsageOnWrongCommand() throws Exception {
        int exitCode = cli.run(new PrintWriter(out), new PrintWriter(err));
        assertEquals(2, exitCode);
        assertThat(err.toString(), CoreMatchers.containsString("Usage: sbomer [-hvV] [COMMAND]"));
    }
}
