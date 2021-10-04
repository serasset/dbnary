#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

# Cleanup maven repo to remove dbnary artifacts that should not be cached.
rm -rf "${HOME}/.m2/repository/org/getalp/*"