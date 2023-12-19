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


source $HOME/.sdkman/bin/sdkman-init.sh
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"  # This loads nvm

# Check if two arguments are provided (version and software type)
if [ $# -lt 2 ]; then
    echo "Usage: $0 <software_type> <desired_version>"
    exit 1
fi

software_type=$1
desired_version=$2

# Check if software_type is valid
if [ "$software_type" != "java" ] && [ "$software_type" != "gradle" ] && [ "$software_type" != "maven" ] && [ "$software_type" != "node" ]; then
    echo "Invalid software type. Must be either 'java', 'gradle', 'maven' or 'node'."
    exit 1
fi

compare_versions() {

    local desired=$1
    local installed_versions=("${@:2}")
    local closest_version=""
    
    IFS='.' read -r -a desired_parts <<< "$desired"

    for installed in "${installed_versions[@]}"; do
        IFS='.' read -r -a installed_parts <<< "$installed"

        # Check for exact match
        if [[ "$desired" == "$installed" ]]; then
            echo "$installed"
            return
        fi

        local is_match=true

        for i in "${!desired_parts[@]}"; do
            if (( ${desired_parts[i]} > ${installed_parts[i]} )); then
                is_match=false
                break
            fi
        done

        if $is_match; then
            closest_version=$installed
            break  # Exit loop once a match is found
        fi
    done

    echo "$closest_version"
}

result=""

if [ "$software_type" == "java" ]; then
    # Check if SDKMAN! is installed
    if [ ! -d "$HOME/.sdkman" ]; then
        echo "SDKMAN! is not installed. Please install it first."
        exit 1
    fi

    # List available Temurin Java versions from SDKMAN!
    output=$(ls -1 "$HOME/.sdkman/candidates/$software_type" | grep "tem")
    # Extract versions using awk with extended regex
    installed_versions=($(echo "$output" | grep -oP "\d+\.\d+(\.\d+)?" | sort -V))
    result=$(compare_versions "$desired_version" "${installed_versions[@]}" 2>/dev/null)
    if [ -n "$result" ]; then
        result="${result}-tem"
    fi

    # Finally check if the result is not null and the directory exists
    if [ -n "$result" ] && [ ! -d "$HOME/.sdkman/candidates/$software_type/$result" ]; then
        # No directory was found, returning empty result
        result=""
    fi

elif [ "$software_type" == "gradle" ] || [ "$software_type" == "maven" ]; then
    # Check if SDKMAN! is installed
    if [ ! -d "$HOME/.sdkman" ]; then
        echo "SDKMAN! is not installed. Please install it first."
        exit 1
    fi

    # List available versions from SDKMAN of the provided software_type!
    output=$(ls -1 "$HOME/.sdkman/candidates/$software_type")
    # Extract versions using awk with extended regex
    installed_versions=($(echo "$output" | grep -oP "\d+\.\d+(\.\d+)?" | sort -V))
    result=$(compare_versions "$desired_version" "${installed_versions[@]}" 2>/dev/null)

    # Finally check if the result is not null and the directory exists
    if [ -n "$result" ] && [ ! -d "$HOME/.sdkman/candidates/$software_type/$result" ]; then
        # No directory was found, returning empty result
        result=""
    fi

else 
    # Check if NVM! is installed (https://github.com/nvm-sh/nvm)
    if [ ! -d "$HOME/.nvm" ]; then
        echo "NVM! is not installed. Please install it first."
        exit 1
    fi

    # List available versions from NVM of node
    output=$(ls -1 "$HOME/.nvm/versions/node/")
    # Extract versions using awk with extended regex
    installed_versions=($(echo "$output" | grep -oP "\d+\.\d+\.\d+" | sort -V))
    result=$(compare_versions "$desired_version" "${installed_versions[@]}" 2>/dev/null)
    if [ -n "$result" ]; then
        result="v${result}"
    fi

    # Finally check if the result is not null and the directory exists
    if [ -n "$result" ] && [ ! -d "$HOME/.nvm/versions/node/$result" ]; then
        # No directory was found, returning empty result
        result=""
    fi

    # Until we know we need to run a node build using the build environment versions, just return the latest installed version.
    # Remove the below line to use the best matching node version.
    result=$(cat "$HOME/.nvmrc")

fi

echo $result


