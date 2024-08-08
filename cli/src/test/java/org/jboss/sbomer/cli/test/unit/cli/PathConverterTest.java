package org.jboss.sbomer.cli.test.unit.cli;

import org.jboss.sbomer.cli.feature.sbom.command.PathConverter;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class PathConverterTest {
    @Test
    void testConvert() throws Exception {
        String userHome = System.getProperty("user.home");
        assertThat(userHome, notNullValue());
        assertThat(new PathConverter().convert("~"), is(Path.of(userHome)));
        assertThat(new PathConverter().convert("~/"), is(Path.of(userHome + "/")));
        // XXX: This one differs from bash if "x" is a user on the system
        assertThat(new PathConverter().convert("~x"), is(Path.of(userHome + "x")));
        assertThat(new PathConverter().convert("/~"), is(Path.of("/~")));
    }
}
