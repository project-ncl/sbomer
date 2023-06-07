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

set NODE_TLS_REJECT_UNAUTHORIZED=0
npmRegistryURL=http://indy-admin.psi.redhat.com/api/folo/track/$buildContentId/npm/group/shared-imports+yarn-public/
noproxy=$(echo $AProxDependencyUrl | sed "s;https\?://;;" | sed "s;/.*;;")
sed -i "s;resolved \"https://registry.yarnpkg.com/;resolved \"$npmRegistryURL;g" ui-pf4/src/main/webapp/yarn.lock
mvn -pl '!tests,!tests/wildfly-dist' clean deploy -DskipTests -Ddownstream=mtr -Dwebpack.environment=production -Dhttp.proxyHost=${proxyServer} -Dhttp.proxyPort=${proxyPort} -Dhttp.proxyUser=${proxyUsername} -Dhttp.proxyPassword=${accessToken} -Dorg.apache.maven.user-settings=/usr/share/maven/conf/settings.xml -DnpmRegistryURL="$npmRegistryURL" -DnpmArgs="--strict-ssl=false --noproxy=${noproxy}"