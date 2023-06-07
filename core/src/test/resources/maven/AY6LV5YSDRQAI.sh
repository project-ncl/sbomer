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

mvn org.apache.maven.plugins:maven-dependency-plugin:get -Dartifact=jboss-eap:jboss-eap-dist:7.4.10.GA:zip || wget --proxy-user=${proxyUsername} --proxy-password=${accessToken} http://download.eng.bos.redhat.com/released/jboss/eap7/7.4.10/jboss-eap-7.4.10.zip && mvn install:install-file -Dfile=jboss-eap-7.4.10.zip -DgroupId=jboss-eap -DartifactId=jboss-eap-dist -Dversion=7.4.10.GA -Dpackaging=zip
unset JAVA_TOOL_OPTIONS
mvn clean deploy -Ddownstream=mtr -DskipTests -Dwildfly.http.port=8081 -Dwebpack.environment=production -Dwildfly.groupId=jboss-eap -Dwildfly.artifactId=jboss-eap-dist -Dversion.wildfly=7.4.10.GA -Dwildfly.directory=jboss-eap-7.4