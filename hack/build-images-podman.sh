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

# This file builds all container images using Podman.

SCRIPT_DIR=$(dirname "$0")

"$SCRIPT_DIR/run-maven.sh" package -DskipTests
"$SCRIPT_DIR/build-ui.sh"

set -x

BUILDER=podman "$SCRIPT_DIR/internal/build-image.sh" "sbomer-service"
BUILDER=podman "$SCRIPT_DIR/internal/build-image.sh" "sbomer-generator"
BUILDER=podman "$SCRIPT_DIR/internal/build-image.sh" "sbomer-ui"

