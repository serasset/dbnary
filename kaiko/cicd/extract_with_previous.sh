#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source ${SCRIPT_DIR}/settings

# Extract data using PR version
DBNARY_DIR=/tmp/$NEXT_VERSION/ ${SCRIPT_DIR}/../extractor/dbnary.sh -V -Z -v $NEXT_VERSION -c $SAMPLE_SIZE $LANGS
