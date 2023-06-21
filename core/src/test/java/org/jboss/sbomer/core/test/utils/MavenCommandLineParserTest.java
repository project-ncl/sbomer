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
package org.jboss.sbomer.core.test.utils;

import org.jboss.sbomer.core.features.sbom.utils.maven.MavenCommandLineParser;
import org.jboss.sbomer.core.test.TestResources;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

@QuarkusTest
public class MavenCommandLineParserTest {

    @Test
    public void windupParentTest() throws IOException, IllegalArgumentException {
        String script = TestResources.asString("maven/AY6LV5YSDRQAM.sh");

        MavenCommandLineParser lineParser = MavenCommandLineParser.build().launder(script);
        assertEquals(2, lineParser.getProfiles().size());
        assertEquals(2, lineParser.getProperties().size());
        assertEquals(0, lineParser.getProjects().size());
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipTests"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Pmtr,mta"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DnpmRegistryURL=\"$npmRegistryURL\""));
    }

    @Test
    public void windupRulesetsTest() throws IOException, IllegalArgumentException {
        String script = TestResources.asString("maven/AY6LV5YSDRQAA.sh");

        MavenCommandLineParser lineParser = MavenCommandLineParser.build().launder(script);
        assertEquals(2, lineParser.getProfiles().size());
        assertEquals(2, lineParser.getProperties().size());
        assertEquals(0, lineParser.getProjects().size());
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipTests=true"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Pmtr,mta"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipThemeWindup=true"));
    }

    @Test
    public void windupMtrCliTest() throws IOException, IllegalArgumentException {
        String script = TestResources.asString("maven/AY6LV5YSDRQAC.sh");

        MavenCommandLineParser lineParser = MavenCommandLineParser.build().launder(script);
        assertEquals(1, lineParser.getProfiles().size());
        assertEquals(7, lineParser.getProperties().size());
        assertEquals(0, lineParser.getProjects().size());
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipTests=true"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Pmtr"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipThemeWindup=true"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dhttp.proxyHost=${proxyServer}"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dhttp.proxyPort=${proxyPort}"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dhttp.proxyUser=${proxyUsername}"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dhttp.proxyPassword=${accessToken}"));
        assertTrue(
                lineParser.getRebuiltMvnCommandScript()
                        .contains("-Dorg.apache.maven.user-settings=/usr/share/maven/conf/settings.xml"));
    }

    @Test
    public void windupWebParentTest() throws IOException, IllegalArgumentException {
        String script = TestResources.asString("maven/AY6LV5YSDRQAE.sh");

        MavenCommandLineParser lineParser = MavenCommandLineParser.build().launder(script);
        assertEquals(0, lineParser.getProfiles().size());
        assertEquals(10, lineParser.getProperties().size());
        assertEquals(1, lineParser.getProjects().size());
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-pl !tests,!tests/wildfly-dist"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipTests"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Ddownstream=mtr"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dwebpack.environment=production "));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dhttp.proxyHost=${proxyServer}"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dhttp.proxyPort=${proxyPort}"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dhttp.proxyUser=${proxyUsername}"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dhttp.proxyPassword=${accessToken}"));
        assertTrue(
                lineParser.getRebuiltMvnCommandScript()
                        .contains("-Dorg.apache.maven.user-settings=/usr/share/maven/conf/settings.xml"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DnpmRegistryURL=\"$npmRegistryURL\""));
        assertTrue(
                lineParser.getRebuiltMvnCommandScript()
                        .contains("-DnpmArgs=\"--strict-ssl=false --noproxy=${noproxy}\""));
    }

    @Test
    public void windupOpenShiftParentTest() throws IOException, IllegalArgumentException {
        String script = TestResources.asString("maven/AY6LV5YSDRQAG.sh");

        MavenCommandLineParser lineParser = MavenCommandLineParser.build().launder(script);
        assertEquals(0, lineParser.getProfiles().size());
        assertEquals(3, lineParser.getProperties().size());
        assertEquals(0, lineParser.getProjects().size());
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipTests"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Djkube.skip"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Ddownstream=mtr"));
    }

    @Test
    public void windupMtrWebDistributionTest() throws IOException, IllegalArgumentException {
        String script = TestResources.asString("maven/AY6LV5YSDRQAI.sh");

        MavenCommandLineParser lineParser = MavenCommandLineParser.build().launder(script);
        assertEquals(0, lineParser.getProfiles().size());
        assertEquals(8, lineParser.getProperties().size());
        assertEquals(0, lineParser.getProjects().size());
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Ddownstream=mtr"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipTests"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dwildfly.http.port=8081"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dwebpack.environment=production"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dwildfly.groupId=jboss-eap"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dwildfly.artifactId=jboss-eap-dist"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dversion.wildfly=7.4.10.GA"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dwildfly.directory=jboss-eap-7.4"));
    }

    @Test
    public void windupMtrMavenPluginTest() throws IOException, IllegalArgumentException {
        String script = TestResources.asString("maven/AY6LV5YSDRQAK.sh");

        MavenCommandLineParser lineParser = MavenCommandLineParser.build().launder(script);
        assertEquals(1, lineParser.getProfiles().size());
        assertEquals(2, lineParser.getProperties().size());
        assertEquals(0, lineParser.getProjects().size());
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Pmtr"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipTests=true"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipThemeWindup=true"));
    }

    @Test
    public void windupOperatorTest() throws IOException, IllegalArgumentException {
        String script = TestResources.asString("maven/AY6LV5YSLRQAA.sh");

        MavenCommandLineParser lineParser = MavenCommandLineParser.build().launder(script);
        assertEquals(1, lineParser.getProfiles().size());
        assertEquals(4, lineParser.getProperties().size());
        assertEquals(0, lineParser.getProjects().size());
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Pnative"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipTests=true"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Ddownstream=mtr"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dquarkus.native.container-build=false"));
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-Dquarkus.native.container-runtime="));
    }

    @Test
    public void tackleDivaCoreTest() throws IOException, IllegalArgumentException {
        String script = TestResources.asString("maven/AY6LV5YSLRQAC.sh");

        MavenCommandLineParser lineParser = MavenCommandLineParser.build().launder(script);
        assertEquals(0, lineParser.getProfiles().size());
        assertEquals(1, lineParser.getProperties().size());
        assertEquals(0, lineParser.getProjects().size());
        assertTrue(lineParser.getRebuiltMvnCommandScript().contains("-DskipTests=true"));
    }
}
