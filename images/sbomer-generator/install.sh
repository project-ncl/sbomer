#!/bin/bash
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

set -e

function install_sdk() {
  curl -s "https://get.sdkman.io" | bash
  source .sdkman/bin/sdkman-init.sh

  echo "sdkman_auto_answer=true" > .sdkman/etc/config
}

function install_java() {
  # 8.0.362 11.0.18
  for java_version in 17.0.7; do
    sdk install java ${java_version}-tem
    sdk install java ${java_version%%.*} "$(sdk home java ${java_version}-tem)"
    sdk use java ${java_version}-tem

    # Of course JDK 8 uses different paths and the keytool command does not have the '-cacerts' flag
    ca_keystore_path="$HOME/.sdkman/candidates/java/${java_version}-tem/jre/lib/security/cacerts"

    if [ ! -f "$ca_keystore_path" ]; then
      ca_keystore_path="$HOME/.sdkman/candidates/java/${java_version}-tem/lib/security/cacerts"
    fi 

    keytool -import -trustcacerts -alias redhat-ca -file /etc/pki/ca-trust/source/anchors/RH-IT-Root-CA.crt -keystore "$ca_keystore_path" -noprompt -storepass changeit
  done
}

function install_maven() {
  for mvn_version in 3.6.3 3.8.8 3.9.2; do
    sdk install maven ${mvn_version}
    sdk install maven ${mvn_version%.*} "$(sdk home maven ${mvn_version})"
  done
}

function install_domino() {
  for domino_version in 0.0.86 0.0.89 0.0.90; do
    curl -L https://github.com/quarkusio/quarkus-platform-bom-generator/releases/download/${domino_version}/domino.jar -o domino-${domino_version}.jar
  done
}

install_sdk
install_java
install_maven
install_domino
