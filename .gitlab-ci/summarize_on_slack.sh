#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"

# shellcheck source=./settings
source "${SCRIPT_DIR}/settings"

env
"$NEXT_VERSION_BIN/dbnary-diff-summary" --discord --next "${DBNARY_CICD_SOURCE_BRANCH}" --previous "${DBNARY_CICD_TARGET_BRANCH}" "$DIFFS"
