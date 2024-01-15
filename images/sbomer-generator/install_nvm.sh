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

cat << 'EOF' >> "$HOME/.bashrc"
export NVM_DIR="$HOME/.nvm"
[ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh" # This loads nvm
[ -s "$NVM_DIR/bash_completion" ] && \. "$NVM_DIR/bash_completion"  # This loads nvm bash_completion
EOF

cat << 'EOF' >> "$HOME/.npmrc"
loglevel=info
maxsockets=80
fetch-retries=10
fetch-retry-mintimeout=60000
EOF

function install_nvm() {
  curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.5/install.sh | bash
  source "${HOME}/.bashrc"
}

function install_nodejs() {
  for nodejs_version in v8.17.0 v10.24.1 v12.22.12 v14.21.3 v16.20.2 v18.19.0 v20.10.0; do
    nvm install ${nodejs_version}
    echo ${nodejs_version} > "$HOME/.nvmrc" # This stores the last version which can be set running 'nvm use'
  done

  # Now that we have a .nvmrc file, we can add 'nvm use' and some npm setup
  echo "nvm use" >> "$HOME/.bashrc"
}

install_nvm
install_nodejs

