/*
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
package org.jboss.sbomer.core.test.unit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.sbomer.core.features.sbom.utils.commandline.CommandLineInspectorUtil;
import org.junit.jupiter.api.Test;

class CommandLineInspectorUtilTest {

    @Test
    void mavenDetectionTest() {
        String script1 = "mvn -Denforcer.skip=true deploy -Dversion.boms.channel.ee=1.0.0.Beta-redhat-00011";
        String script2 = "mvn deploy -DskipTests";
        String script3 = "mvn deploy";
        String script4 = "mvn -B -Pdeploy clean -Denforcer.skip=true -DskipTests -DaltDeploymentRepository=indy-mvn::${AProxDeployUrl}";
        assertTrue(CommandLineInspectorUtil.hasMavenEvidence(script1));
        assertTrue(CommandLineInspectorUtil.hasMavenEvidence(script2));
        assertTrue(CommandLineInspectorUtil.hasMavenEvidence(script3));
        assertTrue(CommandLineInspectorUtil.hasMavenEvidence(script4));
    }

    @Test
    void gradleDetectionTest() {
        String script1 = "# Autobuilder copied this Build Config from BC #13965 rev 2694102\n" + //
                "# This build configuration was modified by Autobuilder\n" + //
                "# Autobuilder copied this Build Config from BC #14101 rev 2691662\n" + //
                "gradle --stacktrace --info  -x check -x javadoc publish";
        String script2 = "gradle --info --no-configuration-cache :dagger:publish";
        String script3 = "gradle --info publish -x autostyleKotlinGradleCheck -x test -x initializeNexusStagingRepository -x javadoc";
        String script4 = "cd server && gradle --no-daemon assemble  publish -Pversion=3.19 --stacktrace";
        String script5 = "gradle --debug publish -x jmh";
        String script6 = "# Autobuilder copied this Build Config from BC #14159 rev 2694372\n" + //
                "export NOGIT=1\n" + //
                "git clone https://code.engineering.redhat.com/gerrit/plume-lib/plume-scripts.git checker/bin-devel/.plume-scripts/\n"
                + //
                "git clone https://code.engineering.redhat.com/gerrit/plume-lib/html-tools.git checker/bin-devel/.html-tools/\n"
                + //
                "git clone https://code.engineering.redhat.com/gerrit/kelloggm/do-like-javac.git checker/bin-devel/.do-like-javac/\n"
                + //
                "git clone https://code.engineering.redhat.com/gerrit/typetools/annotation-tools.git ../annotation-tools/\n"
                + //
                "git clone https://code.engineering.redhat.com/gerrit/typetools/jdk.git ../jdk/\n" + //
                "git clone https://code.engineering.redhat.com/gerrit/mernst/plume-bib.git ../annotation-tools//annotation-file-utilities/plume-bib/\n"
                + //
                "gradle --stacktrace --info publish -x test -x javadoc -x checker-qual:publishCheckerQualPublicationToMavenRepository -x checker:exampleTests -x checker:tutorialTests -x checker:commandLineTests -x annotation-file-utilities:makeAnnotationFileFormat -x spotlessGroovyGradle -x spotlessApply -x spotlessCheck -x check -x framework:copyAndMinimizeAnnotatedJdkFiles -x dataflow:publishDataflowShadedPublicationToGMERepository -x dataflow:publishDataflowShadederrorpronePublicationToGMERepository  -x dataflow:publishDataflowShadednullawayPublicationToGMERepository";
        String script7 = "# Note - mircometer build uses https://docs.gradle.org/current/userguide/toolchains.html.  The build requires Java 11.  If it can't find Java 11 the build will fail with a foojay error as Gradle attempts to auto download a JDK11.\n"
                + //
                "# statsd is skipped owing to ENTMQSTPR-24\n" + //
                "TAG=$(git describe --tags --abbrev=0)\n" + //
                "gradle --stacktrace --debug publish -x check -Prelease.stage=final final  -PcompatibleVersion=SKIP -Prelease.version=${TAG} -Prelease.useLastTag=false -Prelease.disableGitChecks=true -Prelease.ignoreSuppliedVersionVerification -Pskip-statsd=true";
        assertTrue(CommandLineInspectorUtil.hasGradleEvidence(script1));
        assertTrue(CommandLineInspectorUtil.hasGradleEvidence(script2));
        assertTrue(CommandLineInspectorUtil.hasGradleEvidence(script3));
        assertTrue(CommandLineInspectorUtil.hasGradleEvidence(script4));
        assertTrue(CommandLineInspectorUtil.hasGradleEvidence(script5));
        assertTrue(CommandLineInspectorUtil.hasGradleEvidence(script6));
        assertTrue(CommandLineInspectorUtil.hasGradleEvidence(script7));
    }

    @Test
    void npmDetectionTest() {
        String script1 = "export YARN_NPM_REGISTRY_SERVER=\"$(echo \"$AProxDependencyUrl\" | sed \"s;maven/group/$buildContentId;npm/group/shared-imports+yarn-public/;\")\"\n"
                + //
                "export YARN_CA_FILE_PATH=\"/etc/pki/ca-trust/source/anchors/indy-proxy-ca.crt\"\n" + //
                "export YARN_ENABLE_STRICT_SSL=\"false\"\n" + //
                "export YARN_HTTP_TIMEOUT=10000\n" + //
                "export YARN_NODE_LINKER=\"node-modules\"\n" + //
                "\n" + //
                "# Remove embedded Yarn\n" + //
                "rm -r .yarnrc.yml .yarn\n" + //
                "\n" + //
                "# Remove \"private\" to allow PNC to publish to Indy\n" + //
                "sed -i '/\"private\"\\s*:\\s*true/d' package.json\n" + //
                "\n" + //
                "# Generate a .npmignore file to publish dist\n" + //
                "grep -v dist .gitignore > .npmignore\n" + //
                "\n" + //
                "npm install -g corepack\n" + //
                "corepack enable yarn\n" + //
                "yarn install --immutable\n" + //
                "yarn build\n" + //
                "npm publish";
        String script2 = "npm install --fetch-retry-mintimeout 60000 --fetch-retries 10 --maxsockets 5\n" + //
                "npm pack\n" + //
                "npm publish";
        String script3 = "cd ui\n" + //
                "\n" + //
                "# Declare files to be published by NPM and set private=false\n" + //
                "cat package.json | jq '.files = [ \"public/**/*\", \".next/standalone/**/*\", \".next/static/**/*\" ] | .private = false' - | tee package.json\n"
                + //
                "\n" + //
                "npm ci --omit=dev\n" + //
                "npm run build\n" + //
                "npm publish";
        String script4 = "node -v; npm install; npm publish";
        assertTrue(CommandLineInspectorUtil.hasNpmEvidence(script1));
        assertTrue(CommandLineInspectorUtil.hasNpmEvidence(script2));
        assertTrue(CommandLineInspectorUtil.hasNpmEvidence(script3));
        assertTrue(CommandLineInspectorUtil.hasNpmEvidence(script4));
    }

    @Test
    void yarnDetectionTest() {
        String script1 = "export YARN_NPM_REGISTRY_SERVER=\"$(echo \"$AProxDependencyUrl\" | sed \"s;maven/group/$buildContentId;npm/group/shared-imports+yarn-public/;\")\"\n"
                + //
                "export YARN_CA_FILE_PATH=\"/etc/pki/ca-trust/source/anchors/indy-proxy-ca.crt\"\n" + //
                "export YARN_ENABLE_STRICT_SSL=\"false\"\n" + //
                "export YARN_HTTP_TIMEOUT=10000\n" + //
                "export YARN_NODE_LINKER=\"node-modules\"\n" + //
                "\n" + //
                "# Remove embedded Yarn\n" + //
                "rm -r .yarnrc.yml .yarn\n" + //
                "\n" + //
                "# Remove \"private\" to allow PNC to publish to Indy\n" + //
                "sed -i '/\"private\"\\s*:\\s*true/d' package.json\n" + //
                "\n" + //
                "# Generate a .npmignore file to publish dist\n" + //
                "grep -v dist .gitignore > .npmignore\n" + //
                "\n" + //
                "npm install -g corepack\n" + //
                "corepack enable yarn\n" + //
                "yarn install --immutable\n" + //
                "yarn build\n" + //
                "npm publish";
        String script2 = "yarn install --frozen-lockfile\n" + //
                "yarn build\n" + //
                "npm publish";
        String script3 = "# Confirm we can see both node and yarn\n" + //
                "echo \"Version of node installed:\"\n" + //
                "node --version || (echo \"Failed to get version of node\"; exit 1)\n" + //
                "echo \"Version of yarn installed:\"\n" + //
                "yarn --version || (echo \"Failed to get version of yarn\"; exit 1)\n" + //
                "\n" + //
                "# Update yarn config\n" + //
                "yarn config set enableStrictSsl false\n" + //
                "yarn config set httpsCaFilePath /etc/pki/ca-trust/source/anchors/indy-proxy-ca.crt\n" + //
                "yarn config set npmRegistryServer \"${AProxDependencyUrl}/\"\n" + //
                "yarn config set unsafeHttpWhitelist localhost\n" + //
                "\n" + //
                "#\n" + //
                "# Find the last version published in the registry\n" + //
                "#\n" + //
                "PUBLISHED_VERSION=$(yarn npm info @hawtio/react --json | jq -r 'last(.versions[] | select(contains(\"redhat\")))' || echo \"\")\n"
                + //
                "if [ -z \"${PUBLISHED_VERSION}\" ]; then\n" + //
                "  echo \"ERROR: Failed to extract @hawtio/react latest redhat published version\"\n" + //
                "  yarn npm info @hawtio/react --json | jq -r '.'\n" + //
                "  exit 1\n" + //
                "fi\n" + //
                "\n" + //
                "#\n" + //
                "# Find the package version of @hawtio/react\n" + //
                "#\n" + //
                "UPSTREAM_VERSION=$(sed -n 's/.*\"version\": \"\\(.*\\)\"./\\1/p' packages/hawtio/package.json)\n" + //
                "if [ -z \"${UPSTREAM_VERSION}\" ]; then\n" + //
                "  echo \"ERROR: Failed to extract @hawtio/react upstream version\"\n" + //
                "  exit 1\n" + //
                "fi\n" + //
                "\n" + //
                "#\n" + //
                "# Compare the versions and determine the new production version\n" + //
                "#\n" + //
                "\n" + //
                "# Strip the suffix from published version\n" + //
                "PV_BASE=$(echo ${PUBLISHED_VERSION} | sed -n 's/\\([0-9]\\)\\+-.*/\\1/p')\n" + //
                "if [ -z \"${PV_BASE}\" ]; then\n" + //
                "  echo \"ERROR: Failed to extract base semantics of published version\"\n" + //
                "  exit 1\n" + //
                "fi\n" + //
                "\n" + //
                "if [ \"${UPSTREAM_VERSION}\" == \"${PV_BASE}\" ]; then\n" + //
                "  #\n" + //
                "  # UPSTREAM matches PUBLISHED then increment the redhat suffix\n" + //
                "  #\n" + //
                "  PV_SUFFIX=$(echo ${PUBLISHED_VERSION} | sed -n 's/[0-9.]\\+-redhat-\\(.*\\)/\\1/p')\n" + //
                "  if [ -z \"${PV_SUFFIX}\" ]; then\n" + //
                "    echo \"ERROR: Failed to extract suffix of published version\"\n" + //
                "    exit 1\n" + //
                "  fi\n" + //
                "\n" + //
                "  # Increment the published version's suffix (ensure base-10) and pad to 5 digits\n" + //
                "  NEW_SUFFIX=$(printf \"%05d\" $((10#${PV_SUFFIX} + 1)))\n" + //
                "  PROD_VERSION=\"${UPSTREAM_VERSION}-redhat-${NEW_SUFFIX}\"\n" + //
                "else\n" + //
                "  #\n" + //
                "  # UPSTREAM is not the same as PUBLISHED so just append redhat-00001 to UPSTREAM\n" + //
                "  #\n" + //
                "  PROD_VERSION=\"${UPSTREAM_VERSION}-redhat-00001\"\n" + //
                "fi\n" + //
                "\n" + //
                "if [ -z \"${PROD_VERSION}\" ]; then\n" + //
                "  echo \"ERROR: Failed to determine the new production version\"\n" + //
                "  exit 1\n" + //
                "fi\n" + //
                "\n" + //
                "# Update versions in package.json - append redhat suffix\n" + //
                "for p in packages/*\n" + //
                "do\n" + //
                "  sed -i \"s/\\\"version\\\": .*/\\\"version\\\": \\\"${PROD_VERSION}\\\",/\" ${p}/package.json\n" + //
                "\n" + //
                "  pv=$(sed -n 's/.*\\(\"version\":.*\\),/\\1/p' ${p}/package.json)\n" + //
                "  echo \"Package ${p} version updated to ${pv}\"\n" + //
                "done\n" + //
                "\n" + //
                "######################################################################################################\n"
                + //
                "#\n" + //
                "# Update the version of jolokia\n" + //
                "#\n" + //
                "\n" + //
                "JOLOKIA_PINNED_VERSION=2.1.8-redhat-00017\n" + //
                "\n" + //
                "if [ -n \"${JOLOKIA_PINNED_VERSION}\" ]; then\n" + //
                "  JOLOKIA_JS_NPM_VERSION=${JOLOKIA_PINNED_VERSION}\n" + //
                "  JOLOKIA_SIMPLE_NPM_VERSION=${JOLOKIA_PINNED_VERSION}\n" + //
                "else\n" + //
                "  # PNC REST - artifacts\n" + //
                "  PNC_ARTIFACTS_URL=\"http://orch.psi.redhat.com/pnc-rest/v2/artifacts\"\n" + //
                "\n" + //
                "  #\n" + //
                "  # Find the published builds of jolokia to link to the yarn modules\n" + //
                "  #\n" + //
                "  JOLOKIA_JS_IDENTIFIER=$(curl -s \"${PNC_ARTIFACTS_URL}/filter?identifier=jolokia.js%3A%2A&qualities=NEW&repoType=NPM\" | jq -r '.content|last|.identifier')\n"
                + //
                "  if [ -z \"${JOLOKIA_JS_IDENTIFIER}\" ] || [ \"${JOLOKIA_JS_IDENTIFIER}\" == \"null\" ]; then\n" + //
                "    echo \"ERROR: No jolokia.js artifact can be found\"\n" + //
                "    exit 1\n" + //
                "  fi\n" + //
                "  JOLOKIA_SIMPLE_IDENTIFIER=$(curl -s \"${PNC_ARTIFACTS_URL}/filter?identifier=%40jolokia.js%2Asimple%3A%2A&qualities=NEW&repoType=NPM\" | jq -r '.content|last|.identifier')\n"
                + //
                "  if [ -z \"${JOLOKIA_SIMPLE_IDENTIFIER}\" ] || [ \"${JOLOKIA_SIMPLE_IDENTIFIER}\" == \"null\" ]; then\n"
                + //
                "    echo \"ERROR: No @jolokia.js/simple artifact can be found\"\n" + //
                "    exit 1\n" + //
                "  fi\n" + //
                "\n" + //
                "  echo \"Querying for the npm url of the jolokia.js identifier: ${JOLOKIA_JS_IDENTIFIER}\"\n" + //
                "  JOLOKIA_JS_NPM_URL=$(curl -s \"${PNC_ARTIFACTS_URL}?q=identifier%3D%3D${JOLOKIA_JS_IDENTIFIER}\" | jq -r '.content[0].purl')\n"
                + //
                "  if [ -z \"${JOLOKIA_JS_NPM_URL}\" ] || [ \"${JOLOKIA_JS_NPM_URL}\" == \"null\" ]; then\n" + //
                "    echo \"ERROR: No jolokia.js npm published url for artifact ${JOLOKIA_JS_IDENTIFIER} can be found\"\n"
                + //
                "    exit 1\n" + //
                "  fi\n" + //
                "  echo \"Querying for the npm url of the @jolokia.js/simple identifier: ${JOLOKIA_SIMPLE_IDENTIFIER}\"\n"
                + //
                "  JOLOKIA_SIMPLE_NPM_URL=$(curl -s \"${PNC_ARTIFACTS_URL}?q=identifier%3D%3D${JOLOKIA_SIMPLE_IDENTIFIER}\" | jq -r '.content[0].purl')\n"
                + //
                "  if [ -z \"${JOLOKIA_SIMPLE_NPM_URL}\" ] || [ \"${JOLOKIA_SIMPLE_NPM_URL}\" == \"null\" ]; then\n" + //
                "    echo \"ERROR: No @jolokia.js/simple npm published url for artifact ${JOLOKIA_SIMPLE_IDENTIFIER} can be found\"\n"
                + //
                "    exit 1\n" + //
                "  fi\n" + //
                "\n" + //
                "  JOLOKIA_JS_NPM_VERSION=$(echo ${JOLOKIA_JS_NPM_URL} | sed 's/.*@\\(.*\\)/\\1/')\n" + //
                "  JOLOKIA_SIMPLE_NPM_VERSION=$(echo ${JOLOKIA_SIMPLE_NPM_URL} | sed 's/.*@\\(.*\\)/\\1/')\n" + //
                "fi\n" + //
                "\n" + //
                "echo \"Using jolokia.js npm version ${JOLOKIA_JS_NPM_VERSION}\"\n" + //
                "echo \"Using @jolokia/simple npm version ${JOLOKIA_SIMPLE_NPM_VERSION}\"\n" + //
                "\n" + //
                "for module in packages/*\n" + //
                "do\n" + //
                "  echo \"Updating package.json in ${module}\"\n" + //
                "  pushd ${module}\n" + //
                "\n" + //
                "  # Update the package.json of the yarn module to use the\n" + //
                "  # productized jolokia components. Should get the 'latest'\n" + //
                "  # version with a 'pre-release' redhat suffix\n" + //
                "\n" + //
                "  before_js=$(cat package.json | grep \\\"jolokia.js\\\" || echo \"\")\n" + //
                "  if [ -n \"${before_js}\" ]; then\n" + //
                "    sed -i \"s~\\\"jolokia.js\\\": \\\"[\\^|\\~]\\?\\([0-9\\.]\\+\\)\\(-.*\\)\\?\\\"~\\\"jolokia.js\\\": \\\"${JOLOKIA_JS_NPM_VERSION}\\\"~\" package.json\n"
                + //
                "    if [ \"${before_js}\" == \"$(cat package.json | grep \\\"jolokia.js\\\" || echo \"\")\" ]; then\n"
                + //
                "      echo \"ERROR: jolokia.js package not updated!\"\n" + //
                "      exit 1\n" + //
                "    else\n" + //
                "      echo \"jolokia.js package updated correctly.\"\n" + //
                "    fi\n" + //
                "  fi\n" + //
                "\n" + //
                "  before_simple=$(cat package.json | grep @jolokia/simple || echo \"\")\n" + //
                "  if [ -n \"${before_simple}\" ]; then\n" + //
                "    sed -i \"s~\\\"@jolokia.js\\/simple\\\": \\\"[\\^|\\~]\\?\\([0-9\\.]\\+\\)\\(-.*\\)\\?\\\"~\\\"@jolokia.js\\/simple\\\": \\\"${JOLOKIA_SIMPLE_NPM_VERSION}\\\"~\" package.json\n"
                + //
                "    if [ \"${before_simple}\" == \"$(cat package.json | grep @jolokia/simple || echo \"\")\" ]; then\n"
                + //
                "      echo \"ERROR: @jolokia.js/simple package not updated!\"\n" + //
                "      exit 1\n" + //
                "    else\n" + //
                "      echo \"@jolokia.js/simple package updated correctly.\"\n" + //
                "    fi\n" + //
                "  fi\n" + //
                "\n" + //
                "  cat package.json\n" + //
                "\n" + //
                "  popd\n" + //
                "done\n" + //
                "\n" + //
                "######################################################################################################\n"
                + //
                "\n" + //
                "# Try building\n" + //
                "yarn install\n" + //
                "if [ $? -ne 0 ]; then\n" + //
                "  echo \"ERROR: Yarn install failed\"\n" + //
                "  exit 1\n" + //
                "fi\n" + //
                "\n" + //
                "yarn build:all\n" + //
                "if [ $? -ne 0 ]; then\n" + //
                "  echo \"ERROR: Yarn build failed\"\n" + //
                "  exit 1\n" + //
                "fi\n" + //
                "\n" + //
                "# Try publishing\n" + //
                "yarn publish:hawtio\n" + //
                "if [ $? -ne 0 ]; then\n" + //
                "  echo \"ERROR: Something went wrong publishing\"\n" + //
                "  exit 1\n" + //
                "fi";
        assertTrue(CommandLineInspectorUtil.hasYarnEvidence(script1));
        assertTrue(CommandLineInspectorUtil.hasYarnEvidence(script2));
        assertTrue(CommandLineInspectorUtil.hasYarnEvidence(script3));
    }
}
