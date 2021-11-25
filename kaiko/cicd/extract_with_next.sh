#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

# Extract data using Target branch version
DBNARY_DIR="/tmp/$NEXT_VERSION/" "${SCRIPT_DIR}/../extractor/dbnary.sh" -V -Z -n -v "$NEXT_VERSION" -c "$SAMPLE_SIZE" $LANGS

mkdir -p target/extracts/$NEXT_VERSION/
cp /tmp/$NEXT_VERSION/extracts/ontolex/latest/*.ttl target/extracts/$NEXT_VERSION/
