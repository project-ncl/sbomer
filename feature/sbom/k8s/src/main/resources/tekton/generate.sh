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
set -x
set -o pipefail

CONFIG_PATH="/workdir/config.yaml"

# Set the path to MAven settings.xml file so that ict can be used by the generator
export SBOMER_SBOM_SETTINGS_XML_PATH="/workdir/settings.xml"

source /workdir/.sdkman/bin/sdkman-init.sh

echo "Storing configuration in the $CONFIG_PATH file"
echo "$(params.config)" | tee $CONFIG_PATH

echo "Running generation..."
exec /workdir/.sdkman/candidates/java/17/bin/java -jar ./generator/quarkus-run.jar -v sbom auto generate --config $CONFIG_PATH --index "$(params.index)"
