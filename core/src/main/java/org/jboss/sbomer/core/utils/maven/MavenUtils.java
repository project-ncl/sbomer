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
package org.jboss.sbomer.core.utils.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class MavenUtils {

    private static final String AY6LV5YSDRQAM = "npmRegistryURL=http://indy-admin.psi.redhat.com/api/folo/track/$buildContentId/npm/group/shared-imports+yarn-public/"
            + "\nmvn clean deploy -DskipTests -Pmtr,mta -DnpmRegistryURL=\"$npmRegistryURL\"";
    private static final String AY6LV5YSDRQAA = "mvn clean deploy -DskipTests -DskipThemeWindup -Pmtr,mta";
    private static final String AY6LV5YSDRQAC = "JAVA_TOOL_OPTIONS=\"$JAVA_TOOL_OPTIONS -Djdk.http.auth.tunneling.disabledSchemes="
            + "\nmvn clean deploy -DskipTests -DskipThemeWindup -Pmtr -Dhttp.proxyHost=${proxyServer} -Dhttp.proxyPort=${proxyPort} -Dhttp.proxyUser=${proxyUsername} -Dhttp.proxyPassword=${accessToken} -Dorg.apache.maven.user-settings=/usr/share/maven/conf/settings.xml";
    private static final String AY6LV5YSDRQAE = "NODE_TLS_REJECT_UNAUTHORIZED=0\n"
            + "npmRegistryURL=http://indy-admin.psi.redhat.com/api/folo/track/$buildContentId/npm/group/shared-imports+yarn-public/\n"
            + "noproxy=$(echo $AProxDependencyUrl | sed \"s;https\\?://;;\" | sed \"s;/.*;;\")\n"
            + "sed -i \"s;resolved \\\"https://registry.yarnpkg.com/;resolved \\\"$npmRegistryURL;g\" ui-pf4/src/main/webapp/yarn.lock\n"
            + "mvn -pl '!tests,!tests/wildfly-dist' clean deploy -DskipTests -Ddownstream=mtr -Dwebpack.environment=production -Dhttp.proxyHost=${proxyServer} -Dhttp.proxyPort=${proxyPort} -Dhttp.proxyUser=${proxyUsername} -Dhttp.proxyPassword=${accessToken} -Dorg.apache.maven.user-settings=/usr/share/maven/conf/settings.xml -DnpmRegistryURL=\"$npmRegistryURL\" -DnpmArgs=\"--strict-ssl=false --noproxy=${noproxy}\"";
    private static final String AY6LV5YSDRQAG = "mvn clean deploy -DskipTests -Djkube.skip -Ddownstream=mtr";
    private static final String AY6LV5YSDRQAI = "mvn org.apache.maven.plugins:maven-dependency-plugin:get -Dartifact=jboss-eap:jboss-eap-dist:7.4.10.GA:zip || wget --proxy-user=${proxyUsername} --proxy-password=${accessToken} http://download.eng.bos.redhat.com/released/jboss/eap7/7.4.10/jboss-eap-7.4.10.zip && mvn install:install-file -Dfile=jboss-eap-7.4.10.zip -DgroupId=jboss-eap -DartifactId=jboss-eap-dist -Dversion=7.4.10.GA -Dpackaging=zip"
            + "\nunset JAVA_TOOL_OPTIONS"
            + "\nmvn clean deploy -Ddownstream=mtr -DskipTests -Dwildfly.http.port=8081 -Dwebpack.environment=production -Dwildfly.groupId=jboss-eap -Dwildfly.artifactId=jboss-eap-dist -Dversion.wildfly=7.4.10.GA -Dwildfly.directory=jboss-eap-7.4";
    private static final String AY6LV5YSDRQAK = "mvn clean deploy -DskipTests -DskipThemeWindup -Pmtr";
    private static final String AY6LV5YSLRQAA = "export GRAALVM_HOME=/opt/mandrel-java11-20.3.0.0.Final"
            + "\nmvn clean deploy -DskipTests -Ddownstream=mtr -Pnative -Dquarkus.native.container-build=false -Dquarkus.native.container-runtime=";
    private static final String AY6LV5YSLRQAC = "JAVA_TOOL_OPTIONS=\"$JAVA_TOOL_OPTIONS -Djdk.http.auth.tunneling.disabledSchemes=\""
            + "\nmkdir ~/.gradle" + "\ncat << EOF > ~/.gradle/gradle.properties"
            + "\nsystemProp.https.proxyHost=${proxyServer}" + "\nsystemProp.https.proxyPort=${proxyPort}"
            + "\nsystemProp.https.proxyUser=${proxyUsername}" + "\nsystemProp.https.proxyPassword=${accessToken}"
            + "\nEOF" + "\nmvn clean deploy -DskipTests";

    public static void main(String[] args) {

        String cmd = AY6LV5YSDRQAE;

        try {
            MavenCommandLineParser lineParser = MavenCommandLineParser.build().launder(cmd);
            System.out.println(lineParser);

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        // System.out.println("Parsed: \n```\n" + cmd + "\n```");
        // System.out.println("Profiles: " + parser.getProfiles());
        // System.out.println("Properties: " + parser.getProperties());
        // System.out.println("Options: " + parser.getOptions());
        // System.out.println("Modules: " + parser.getModules());
        // System.out.println("Enabled: " + parser.getEnabledOptions());
    }

    /*
     *
     * options.addOption(Option.builder("f").longOpt("file").hasArg(true).
     * desc("Force the use of an alternate POM file (or directory with pom.xml)").build());
     *
     *
     * //
     */
    // public static void main(String[] args) {

    // // Specify the path to your Maven project's pom.xml file
    // File pomFile = new File("/home/avibelli/Downloads/TEST_TC/windup-web-21bab6d/pom.xml");

    // // Create a ModelBuilder instance
    // ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();

    // // Build the Maven project model
    // ModelBuildingRequest request = new DefaultModelBuildingRequest()
    // .setPomFile(pomFile);
    // try {
    // ModelBuildingResult result = modelBuilder.build(request);

    // // Get the project model
    // Model projectModel = result.getEffectiveModel();

    // // Get the profiles defined in the project model
    // List<Profile> profiles = projectModel.getProfiles();

    // // List the profile IDs
    // System.out.println("Profiles:");
    // for (Profile profile : profiles) {
    // System.out.println(profile.getId());
    // }
    // } catch (ModelBuildingException e) {
    // System.err.println("Failed to build Maven project model: " + e.getMessage());
    // }
    /*********************************************** */
    // // Create a ModelBuilder instance
    // DefaultModelBuilderFactory factory = new DefaultModelBuilderFactory();
    // ModelBuilder modelBuilder = factory.newInstance();

    // // Create a ModelBuildingRequest with a ModelResolver
    // ModelBuildingRequest request = new DefaultModelBuildingRequest()
    // .setPomFile(pomFile);
    // ModelResolver modelResolver = new DefaultModelResolver();
    // request.setModelResolver(modelResolver);

    // // Build the Maven project model
    // try {
    // ModelBuildingResult result = modelBuilder.build(request);

    // // Get the project model
    // Model projectModel = result.getEffectiveModel();

    // // Get the profiles defined in the project model
    // List<Profile> profiles = projectModel.getProfiles();

    // // List the profile IDs
    // System.out.println("Profiles:");
    // for (Profile profile : profiles) {
    // System.out.println(profile.getId());
    // }
    // } catch (ModelBuildingException e) {
    // System.err.println("Failed to build Maven project model: " + e.getMessage());
    // } catch (UnresolvableModelException e) {
    // System.err.println("Failed to resolve Maven project model: " + e.getMessage());
    // }
    /*********************************************** */

    // // Create a RepositorySystemSession
    // RepositorySystem repositorySystem = new RepositorySystem();
    // RepositorySystemSession repositorySession = new DefaultRepositorySystemSession();

    // // Create a ModelBuilder instance
    // ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();

    // // Create a ModelBuildingRequest with a ModelResolver
    // ModelBuildingRequest request = new DefaultModelBuildingRequest()
    // .setPomFile(pomFile)
    // .setRepositorySession(repositorySession);
    // org.apache.maven.model.resolution.ModelResolver modelResolver = new DefaultModelResolver(repositorySystem,
    // repositorySession, null);
    // request.setModelResolver(modelResolver);

    // // Build the Maven project model
    // try {
    // ModelBuildingResult result = modelBuilder.build(request);

    // // Get the project model
    // Model projectModel = result.getEffectiveModel();

    // // Get the profiles defined in the project model
    // List<Profile> profiles = projectModel.getProfiles();

    // // List the profile IDs
    // System.out.println("Profiles:");
    // for (Profile profile : profiles) {
    // System.out.println(profile.getId());
    // }
    // } catch (ModelBuildingException e) {
    // System.err.println("Failed to build Maven project model: " + e.getMessage());
    // }

    // }
}
