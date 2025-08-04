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

curl -s "https://get.sdkman.io" | bash
sed -i -e "s|^sdkman_auto_env=false$|sdkman_auto_env=true|" "$HOME/.sdkman/etc/config"
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.12-tem
