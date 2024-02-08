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

if [ x"${APP_ENV}" == "xdev" ]; then
    export QUARKUS_OIDC_CLIENT_CLIENT_ENABLED=false
else
    if [ -n "${SBOMER_SECRET_NAME}" ]; then
        export QUARKUS_OIDC_CLIENT_CREDENTIALS_SECRET="$(cat /mnt/secrets/${SBOMER_SECRET_NAME}-${APP_ENV}/pnc-sbomer-client.secret)"
    fi
fi

export CONFIG_PATH="${HOME}/config.yaml"
export ENV_CONFIG_PATH="${HOME}/env-config.json"
# Set the path to Maven settings.xml file so that it can be used by the generator
export SBOMER_MAVEN_SETTINGS_XML_PATH="${HOME}/settings.xml"
# Set the path to Gradle init script file so that it can be used by the generator
export SBOMER_GRADLE_SETTINGS_XML_PATH="${HOME}/cyclonedx-init.gradle"

export MAVEN_OPTS="-Xms256m -Xmx512m"
