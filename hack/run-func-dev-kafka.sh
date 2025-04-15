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

# Requires kcat binary!

SCRIPT_DIR=$(dirname "$0")

set -x

BROKER="localhost:9092"
TOPIC="generation-request"

HEADER_SPECVERSION="ce_specversion=1.0"
HEADER_TYPE="ce_type=org.jboss.sbomer.generation.request.image.v1alpha1"
HEADER_SOURCE="ce_source=https://sbomer/handler/umb"
HEADER_ID="ce_id=$(uuidgen)"

EVENT_DATA='{"spec":{"image": "registry-proxy.engineering.redhat.com/rh-osbs/openshift-ose-cluster-cloud-controller-manager-operator@sha256:4a0c89af8c0cbaafd369b505b7783eab15cd6f5af1c5ffcf0f424aacadc4dca9"}}'

echo "$EVENT_DATA" | kcat -X broker.address.family=v4 -P -b "$BROKER" -t "$TOPIC" \
  -H "$HEADER_SPECVERSION" \
  -H "$HEADER_TYPE" \
  -H "$HEADER_SOURCE" \
  -H "$HEADER_ID"
