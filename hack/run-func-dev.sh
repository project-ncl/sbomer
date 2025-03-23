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

# A very simple wrapper around Maven execution to make it more reusable.

SCRIPT_DIR=$(dirname "$0")

set -x

URL=http://localhost:8080/

EVENT_DATA='{"type": "org.jboss.sbomer.generation.request.zip.v1alpha1", "spec":{"url": "http://host.com/path/to/zip.zip"}}'

exec curl -v --http2-prior-knowledge --ipv4 ${URL} \
  -H "content-type:application/json" \
  -H "accept:application/json" \
  -H "ce-id: $(uuidgen)" \
  -H "ce-source: https://sbomer/handler/umb" \
  -H "ce-type: org.jboss.sbomer.generation.request.zip.v1alpha1" \
  -H "ce-specversion: 1.0" \
  -d "$EVENT_DATA"

