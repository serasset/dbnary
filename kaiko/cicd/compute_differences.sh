#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source ${SCRIPT_DIR}/settings

# compute differences
echo ${SCRIPT_DIR}/../extractor/compute_diffs.sh -v $PREVIOUS_VERSION -f /tmp/$PREVIOUS_VERSION/extracts/ontolex/latest/ -t /tmp/$NEXT_VERSION/extracts/ontolex/latest/ -d $DIFFS $LANGS
${SCRIPT_DIR}/../extractor/compute_diffs.sh -v $PREVIOUS_VERSION -f /tmp/$PREVIOUS_VERSION/extracts/ontolex/latest/ -t /tmp/$NEXT_VERSION/extracts/ontolex/latest/ -d $DIFFS $LANGS

# Move diffs to the build dir as artifacts are only possible there
mkdir -p target/
mv /tmp/diffs target/diffs