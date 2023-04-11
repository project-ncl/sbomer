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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.inject.Inject;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.jboss.pnc.dto.Build;
import org.jboss.sbomer.cli.CLI;
import org.jboss.sbomer.cli.service.PNCService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;

@QuarkusTest
public class CycloneDXPluginMavenGenerateCommandTest {
    @Inject
    CLI cli;

    @InjectMock
    PNCService pncService;

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
    @Disabled("Integration testing is not that easy, but leaving here for now")
    void shouldRunGeneration() throws Exception {
        try (MockedStatic<Git> git = Mockito.mockStatic(Git.class)) {
            git.when(Git::cloneRepository).thenReturn(Mockito.mock(CloneCommand.class, RETURNS_DEEP_STUBS));
        }

        Mockito.mockStatic(Git.class);
        Mockito.when(pncService.getBuild("AAABBB"))
                .thenReturn(Build.builder().scmUrl("scmurl").scmTag("scmtag").build());

        int exitCode = cli
                .run(new PrintWriter(out), new PrintWriter(err), "generate", "--build-id", "AAABBB", "maven", "cdx");

        assertEquals(err.toString(), "");
        assertEquals(0, exitCode);
    }
}
