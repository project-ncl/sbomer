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

set -e
set -o pipefail

script=$(basename "$0")

if [ "$#" -ne 3 ]; then
    echo "Illegal number of parameters, provided $#, required: 3"
    echo
    echo "Usage:"
    echo "    ${script} [CONFIG_JSON] [WORKDIR_PATH] [INDEX]"
    echo
    echo "Example:"
    echo "    ${script} config.json workdir 0"
    exit 1
fi

config_path="${1}"
workdir_path="${2}"
index="${3}"

certs_path="/etc/pki/ca-trust/source/anchors"

# This loads NVM and SDKMAN!
source "${HOME}/.bashrc"

if [ -n "${DEBUG}" ]; then
    set -x
fi

# Install required generator
generator=$(jq -r ".product.generator.type" ${config_path})

case "$generator" in
"maven-cyclonedx-operation")
    echo "The ${generator} generator does not require installation"
    ;;
*)
    echo "Unexpected ${generator}! It could not be installed, exiting"
    exit 1
    ;;
esac

# SBOMer environment
source "${HOME}/env.sh"

echo "Running operation generation..."
exec "${HOME}/.sdkman/candidates/java/17/bin/java" -Duser.home=/workdir -Xms256m -Xmx512m -jar /workdir/generator/quarkus-run.jar -v sbom auto generate-operation --workdir "${workdir_path}" --config "${config_path}" --index "${index}"
        
