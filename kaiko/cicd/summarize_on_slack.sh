#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

"${SCRIPT_DIR}/../extractor/summarize_diffs.sh" -v "$NEXT_VERSION" -d $DIFFS -s
