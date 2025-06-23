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
set -o pipefail

SBOMER_JDK_VERSION="17.0.12-tem"
NODEJS_VERSION="lts/iron"
SYFT_VERSION="1.27.1"
SBT_VERSION="1.10.7"

# SBOMer functions
source "${HOME}/func.sh"

function install_syft() {
    local version=${1}

    mkdir -p ${HOME}/syft
    curl -s -L https://github.com/anchore/syft/releases/download/v${version}/syft_${version}_linux_amd64.tar.gz | tar xvz -C "${HOME}/syft"
}

install_sdk
install_nvm

# Activate nvm and sdk
source "${HOME}/.bashrc"

install_syft "${SYFT_VERSION}"
# Install Java 17 -- required to run SBOMer itself
install_java "${SBOMER_JDK_VERSION}"
# Install nodejs -- it will be done only once
install_nodejs "${NODEJS_VERSION}"
# Install Yarn -- needs to be done after Node.js and NPM are installed
install_yarn
# Install SBT
install_sbt "${SBT_VERSION}"

mkdir -p "${HOME}/.npm/_cacache"
chown -R 65532:0 "${HOME}"
chmod -R g=u "${HOME}"
