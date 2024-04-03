#!/bin/env bash

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

if [ "$#" -ne 2 ]; then
    echo "Illegal number of parameters, provided $#, required: 2"
    echo
    echo "Usage:"
    echo "    ${0} [PREVIOUS_RELEASE_HASH] [LATEST_RELEASE_HASH]"
    echo
    echo "Example:"
    echo "    ${0} adcc6d1 307f4724"
    exit 1
fi

KUBE_CONTEXT="${KUBE_CONTEXT:-aws-prod}"

release_from="${1}"
release_to="${2}"

export TZ=":UTC"

echo "---- Release history ----"
helm --kube-context "${KUBE_CONTEXT}" -n sbomer--runtime-int history sbomer

DEPLOYMENT_DATE=$(date -d "$(helm --kube-context "${KUBE_CONTEXT}" -n sbomer--runtime-int get metadata sbomer | grep DEPLOYED_AT | awk -F': ' '{ print $2 }')" +"%Y-%m-%d %H:%M %Z")

echo
echo "---- Commits ----"

git log "${release_from}..${release_to}" --oneline --no-merges --pretty="format:%h (%an) %s"

echo
echo "---- Release Notes Entry ----"
echo "## :date: ${DEPLOYMENT_DATE}"
echo "### Changes"
echo "- TBD"
echo "### Details"
echo "- Release \`${release_to}\`"
echo "- [Code](https://github.com/project-ncl/sbomer/tree/${release_to})"
echo "- [Commits](https://github.com/project-ncl/sbomer/compare/${release_from}...${release_to})"
