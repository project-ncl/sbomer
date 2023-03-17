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

# This file builds all container images and makes these available in the
# Minikube environemt. This is useful in case of deployment of the 'local'
# environment/overrides.
#
# This script should be run after you set up the Minikube environment.

SCRIPT_DIR=$(dirname "$0")

"$SCRIPT_DIR/run-maven.sh" package -DskipTests

minikube -p sbomer image build -t localhost/sbomer-service:latest -f src/main/images/service/Containerfile.jvm "$SCRIPT_DIR/../"
minikube -p sbomer image build -t localhost/sbomer-generator:latest -f src/main/images/generator/Containerfile "$SCRIPT_DIR/../"