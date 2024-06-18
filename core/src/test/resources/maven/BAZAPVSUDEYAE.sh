#
# JBoss, Home of Professional Open Source.
# Copyright 2023 Red Hat, Inc., and individual contributors
# as indicated by the @author tags.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Djdk.http.auth.tunneling.disabledSchemes="
mvn org.eclipse.tycho:tycho-versions-plugin:3.0.0:update-eclipse-metadata -Dtycho.disableP2Mirrors=true -Dhttp.proxyHost=${proxyServer} -Dhttp.proxyPort=${proxyPort} -Dhttp.proxyUser=${proxyUsername} -Dhttp.proxyPassword=${accessToken} 
mvn clean deploy -DskipTests -Dtycho.strictVersions=false -Dtycho.disableP2Mirrors=true -Dhttp.proxyHost=${proxyServer} -Dhttp.proxyPort=${proxyPort} -Dhttp.proxyUser=${proxyUsername} -Dhttp.proxyPassword=${accessToken}