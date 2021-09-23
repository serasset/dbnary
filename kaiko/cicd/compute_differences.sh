#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source ${SCRIPT_DIR}/settings

# compute differences
${SCRIPT_DIR}/../extractor/compute_diff.sh -v $PREVIOUS_VERSION -f /tmp/$PREVIOUS_VERSION -t /tmp/$NEXT_VERSION -d $DIFFS $LANGS

# Cleanup maven repo to remove dbnary artifacts that should not be cached.
rm -rf ${HOME}/.m2/repository/org/getalp/*