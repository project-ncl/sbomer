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

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters, provided $#, required: 1"
    echo
    echo "Usage:"
    echo "    ${0} [LATEST_RELEASE_COMMIT_HASH]"
    echo
    echo "Example:"
    echo "    ${0} adcc6d1"
    exit 1
fi

git fetch upstream > /dev/null

COMMIT=$(git rev-parse --short upstream/main)

export TZ=":UTC"

DEPLOYMENT_DATE=$(date -d "$(helm --kube-context sbomer-prod get metadata sbomer | grep DEPLOYED_AT | awk -F': ' '{ print $2 }')" +"%Y-%m-%d %H:%M %Z")

echo "---- Commits ----"

git log "${1}..${COMMIT}" --oneline --no-merges --pretty="format:%h (%an) %s"

echo
echo "---- Release Notes Entry ----"
echo "##:jira-major: :jira-minor: :date: ${DEPLOYMENT_DATE}"
echo "### Changes"
echo "- TBD"
echo "### Details"
echo "- Release \`${COMMIT}\`"
echo "- [Code](https://github.com/project-ncl/sbomer/tree/${COMMIT})"
echo "- [Commits](https://github.com/project-ncl/sbomer/compare/${1}...${COMMIT})"
