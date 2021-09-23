#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source ${SCRIPT_DIR}/settings

# compute differences
${SCRIPT_DIR}/../extractor/compute_diffs.sh -v $PREVIOUS_VERSION -f /tmp/$PREVIOUS_VERSION -t /tmp/$NEXT_VERSION -d $DIFFS $LANGS
