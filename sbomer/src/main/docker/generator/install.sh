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


function install_sdk() {
  curl -s "https://get.sdkman.io" | bash
  source .sdkman/bin/sdkman-init.sh
}

function install_java() {
  for java_version in 8.0.362 11.0.18 17.0.6; do
    sdk install java ${java_version}-tem
    sdk install java ${java_version%%.*} "$(sdk home java ${java_version}-tem)"
  done
}

function install_maven() {
  for mvn_version in 3.6.3 3.8.7 3.9.0; do
    sdk install maven ${mvn_version}
    sdk install maven ${mvn_version%.*} "$(sdk home maven ${mvn_version})"
  done
}

function install_domino() {
  curl -L https://github.com/quarkusio/quarkus-platform-bom-generator/releases/download/0.0.77/domino.jar -o domino.jar
}

function install_cert() {
  curl -L https://password.corp.redhat.com/RH-IT-Root-CA.crt -o RH-IT-Root-CA.crt
  keytool -import -trustcacerts -alias redhat-ca -file RH-IT-Root-CA.crt -cacerts -noprompt -storepass changeit
}

install_sdk
install_java
install_maven
install_cert
install_domino
