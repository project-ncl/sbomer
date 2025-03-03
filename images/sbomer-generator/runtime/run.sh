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
    echo "    ${script} [CONFIG_JSON] [ENV_CONFIG_JSON] [INDEX]"
    echo
    echo "Example:"
    echo "    ${script} config.json env-config.json 0"
    exit 1
fi

config_path="${1}"
env_config_path="${2}"
index="${3}"

certs_path="/etc/pki/ca-trust/source/anchors"

# This loads NVM and SDKMAN!
source "${HOME}/.bashrc"
# SBOMer functions
source "${HOME}/func.sh"

if [ -n "${DEBUG}" ]; then
    set -x
fi

# Install required tools
## Iterate over the evironment configuration file and find any tools and versions to install
for tool in $(jq -r '[keys[]] | join(" ")' ${env_config_path}); do
    version=$(jq -r ".[\"${tool}\"]" ${env_config_path})

    echo "About to install '${tool}' with version '${version}'"

    case "$tool" in
    "java")
        # Special case, we have only major version
        retry install_major_java "${version}"
        echo "java=${version}" >>".sdkmanrc"
        ;;
    "node")
        nodeversion=$(cat $HOME/.nvmrc)
        # Special case -- we have nodejs installed already
        echo "Skipping installing Node '${version}', we will use already installed version '${nodeversion}'"
        nvm use "${nodeversion}"
        ;;
    *)
        # All other tools, rely on SDKMAN!
        retry sdk install "${tool}" "${version}"
        echo "${tool}=${version}" >>".sdkmanrc"
        ;;
    esac
done

# Source again so that tools listed in .sdkmanrc will be used
source "${HOME}/.bashrc"
# SBOMer functions
source "${HOME}/func-gen.sh"

if [ -f ".sdkmanrc" ]; then
    # Initialize SDKMAN! environment
    sdk env
fi

# Install required generator
generator=$(jq -r ".products[${index}].generator.type" ${config_path})
version=$(jq -r ".products[${index}].generator.version" ${config_path})

case "$generator" in
"maven-domino")
    install_domino "${version}"
    ;;
"npm-cyclonedx")
    install_npm_cyclonedx "${version}"
    ;;
"gradle-cyclonedx" | "maven-cyclonedx" | "yarn-cyclonedx" | "sbt-cyclonedx")
    echo "The ${generator} generator does not require installation"
    ;;
*)
    echo "Unexpected ${generator}! It could not be installed, exiting"
    exit 1
    ;;
esac

# SBOMer environment
source "${HOME}/env.sh"

# Ensure that we use a specifc version for Domino
#
# See: https://issues.redhat.com/browse/SBOMER-69
export DOMINO_JAVA_BIN="${HOME}/.sdkman/candidates/java/17/bin/java"

echo "Running generation..."
exec "${HOME}/.sdkman/candidates/java/17/bin/java" -XX:InitialRAMPercentage=15.0 -XX:MaxRAMPercentage=15.0 -XX:+ExitOnOutOfMemoryError -XX:+PrintCommandLineFlags -XshowSettings:vm -Duser.home=/workdir -jar /workdir/generator/quarkus-run.jar -v sbom auto generate --workdir /tmp/sbomer-workdir --config "${config_path}" --index "${index}"
