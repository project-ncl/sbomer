#!/bin/bash

set -x
set -e

if [ -f "/mnt/secrets/env.sh" ]; then
    cp /mnt/secrets/env.sh /deplyoments/run-env.sh
fi

exec /usr/local/s2i/run
