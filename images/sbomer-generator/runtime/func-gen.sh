#!/usr/bin/env bash
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

# Source again so that tools listed in .sdkmanrc will be used
source "${HOME}/.bashrc"

# Installs Domino.
function install_domino() {
    echo "> Installing Domino CycloneDX SBOM generator version '${1}'"

    # Ensure the target directory exists
    mkdir -p "${SBOMER_DOMINO_DIR}"

    curl -s -L https://github.com/quarkusio/quarkus-platform-bom-generator/releases/download/${1}/domino.jar -o ${SBOMER_DOMINO_DIR}/domino-${1}.jar

    echo "Domino installed!"
}

function install_npm_cyclonedx() {
    echo "Installing @cyclonedx/cyclonedx-npm NPM package version '${1}'"
    npm install --global "@cyclonedx/cyclonedx-npm@${1}"
    echo "NPM package @cyclonedx/cyclonedx-npm installed!"
}
