#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source ${SCRIPT_DIR}/settings

# Extract data using Target branch version
DBNARY_DIR=/tmp/$PREVIOUS_VERSION/ dbnary.sh -V -v $PREVIOUS_VERSION -c $SAMPLE_SIZE $LANGS
