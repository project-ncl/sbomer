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

# Run the CLI with "--help". It can be overriden by providing arguments to this script.

SCRIPT_DIR=$(dirname "$0")

set -x

ARGS="--help"

if [[ $# -ne 0 ]]; then
	ARGS="$*"
fi

exec "$SCRIPT_DIR/run-maven.sh" -pl cli -am quarkus:dev -Dquarkus.args="${ARGS}"
