#!/usr/bin/env bash

BATCH_LANGS=$@

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

EFFECTIVE_LANGS=(`echo $BATCH_LANGS $LANGS | tr ' ' '\n' | sort | uniq -d`)
echo "Effectively extracting languages : " $BATCH_LANGS "//" $LANGS "-->" ${EFFECTIVE_LANGS[@]}

[[ x$EFFECTIVE_LANGS == x ]] && exit 0;

# Extract data using Target branch version
DBNARY_DIR="/tmp/$NEXT_VERSION/" "${SCRIPT_DIR}/../extractor/dbnary.sh" -V -Z -n -v "$NEXT_VERSION" -c "$SAMPLE_SIZE" ${EFFECTIVE_LANGS[@]}

mkdir -p target/extracts/$NEXT_VERSION/
cp /tmp/$NEXT_VERSION/extracts/ontolex/latest/*.ttl target/extracts/$NEXT_VERSION/
