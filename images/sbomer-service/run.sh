#!/bin/bash
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

if [ "${SCRIPT_DEBUG}" = "true" ] ; then
    set -x
    echo "Script debugging is enabled"
fi

export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-"-XX:InitialRAMPercentage=20.0 -XX:MaxRAMPercentage=50.0 -XX:+ExitOnOutOfMemoryError -XX:NativeMemoryTracking=summary"}"

exec /usr/local/s2i/run
