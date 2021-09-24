#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source ${SCRIPT_DIR}/settings

# compute differences
${SCRIPT_DIR}/../extractor/compute_diffs.sh -v $PREVIOUS_VERSION -f /tmp/$PREVIOUS_VERSION/extracts/ontolex/latest/ -t /tmp/$NEXT_VERSION/extracts/ontolex/latest/ -d $DIFFS $LANGS

ls -al $DIFFS