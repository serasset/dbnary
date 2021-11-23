#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

for model in ontolex morphology etymology enhancement
do
  # compute differences
  echo "${SCRIPT_DIR}/../extractor/compute_diffs.sh -v $NEXT_VERSION -f /tmp/$PREVIOUS_VERSION/extracts/ontolex/latest/ -t /tmp/$NEXT_VERSION/extracts/ontolex/latest/ -d $DIFFS $LANGS"
  "${SCRIPT_DIR}/../extractor/compute_diffs.sh" -v "$NEXT_VERSION" -f "/tmp/$PREVIOUS_VERSION/extracts/ontolex/latest/" -t "/tmp/$NEXT_VERSION/extracts/ontolex/latest/" -d "$DIFFS" $LANGS
done

# Move diffs to the build dir as artifacts are only possible there
mkdir -p target/
mv /tmp/diffs target/diffs
