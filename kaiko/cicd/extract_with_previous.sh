#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

set -x

# Extract data using PR version
"DBNARY_DIR=/tmp/$PREVIOUS_VERSION/" "${SCRIPT_DIR}/../extractor/dbnary.sh" -V -Z -n -v "$PREVIOUS_VERSION" -c "$SAMPLE_SIZE" $LANGS
