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

# Builds specific image and pushes to registry.
# Although this script can be run directly, it's main usage is to be executed from other scripts.
#
# Usage:
#  - build-image.sh [IMAGE_SLUG]

if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters, provided $#, required: 1"
    echo
    echo "Usage:"
    echo "    build-image.sh [IMAGE_SLUG]"
    echo
    echo "Example:"
    echo "    build-image.sh sbomer-service"
    exit 1
fi

# Use podman as the default builder
BUILDER="podman"

# Handle the PUSH env var
# Don't push by default
if [ -z "$PUSH" ]; then
  PUSH="no"
fi

# Handle the IMAGE_REGISTRY env var
if [ -z "$IMAGE_REGISTRY" ] || [ "$IMAGE_REGISTRY" = "localhost" ]; then
  IMAGE_REGISTRY="localhost"
fi

# Handle the BUILDER env var
case "$BUILDER" in
podman)
  BUILD_SCRIPT=("podman")
  ;;
minikube)
  BUILD_SCRIPT=("minikube -p sbomer image")
  ;;
*)
  echo "Unrecognized builder specified, using Podman"
  BUILD_SCRIPT=("podman")
esac

# Set up required variables
SCRIPT_DIR=$(dirname "$0")
SHORTCOMMIT=$(git rev-parse --short HEAD)

IMAGE_SLUG="${1}"
IMAGE_TAG_LATEST="${IMAGE_REGISTRY}/${IMAGE_SLUG}:latest"
IMAGE_TAG_COMMIT="${IMAGE_REGISTRY}/${IMAGE_SLUG}:${SHORTCOMMIT}"

set -x

pushd "$SCRIPT_DIR/../" > /dev/null

"${BUILD_SCRIPT[@]}" build -t "$IMAGE_TAG_LATEST" -f "src/main/images/${IMAGE_SLUG}/Containerfile" .

if [ "$PUSH" = "yes" ]; then
"${BUILD_SCRIPT[@]}" tag "$IMAGE_TAG_LATEST" "$IMAGE_TAG_COMMIT"
"${BUILD_SCRIPT[@]}" push "$IMAGE_TAG_LATEST"
"${BUILD_SCRIPT[@]}" push "$IMAGE_TAG_COMMIT"
fi

"${BUILD_SCRIPT[@]}" inspect "$IMAGE_TAG_LATEST" > "target/image-${IMAGE_SLUG}.json"

popd > /dev/null