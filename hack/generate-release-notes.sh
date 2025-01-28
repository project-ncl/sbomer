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

if [ "$#" -ne 0 ]; then
    echo "Illegal number of parameters, provided $#, required: 0"
    echo
    echo "Usage:"
    echo "    ${0}"
    exit 1
fi

KUBE_CONTEXT="${KUBE_CONTEXT:-aws-prod}"
REVISIONS=5

export TZ=":UTC"

echo "---- Release history ----"
history=$(helm --kube-context "${KUBE_CONTEXT}" -n sbomer--runtime-int history --max ${REVISIONS} sbomer 2>/dev/null)

echo "$history"

last_rev=$(echo "$history" | tail -1 | awk '{ print $1 }')

for rev in $(echo "$history" | tail -${REVISIONS} | awk '{ print $1 }'); do
    prev=$((rev - 1))

    echo
    echo "Revision ${rev}"
    echo
    echo "---- Commits ----"

    deployment_date=$(date -d "$(helm --kube-context "${KUBE_CONTEXT}" -n sbomer--runtime-int get metadata sbomer --revision "${rev}" 2>/dev/null | grep DEPLOYED_AT | awk -F': ' '{ print $2 }')" +"%Y-%m-%d %H:%M %Z")
    release_to=$(helm --kube-context "${KUBE_CONTEXT}" -n sbomer--runtime-int get notes sbomer --revision "${rev}" 2>/dev/null | grep "Application version" | awk -F': ' '{ print $2 }' | sed 's/[\.\ ]//g')
    release_from=$(helm --kube-context "${KUBE_CONTEXT}" -n sbomer--runtime-int get notes sbomer --revision "${prev}" 2>/dev/null | grep "Application version" | awk -F': ' '{ print $2 }' | sed 's/[\.\ ]//g')

    git log "${release_from}..${release_to}" --oneline --no-merges --pretty="format:%h (%an) %s"

    echo
    echo "---- Release Notes Entry ----"
    echo "### :date: ${deployment_date}"
    echo "#### Changes"
    echo "- TBD"
    echo "#### Details"
    echo "- Release \`${release_to}\`"
    echo "- Revision ${revision}"
    echo "- [Code](https://github.com/project-ncl/sbomer/tree/${release_to})"
    echo "- [Commits](https://github.com/project-ncl/sbomer/compare/${release_from}...${release_to})"
done



