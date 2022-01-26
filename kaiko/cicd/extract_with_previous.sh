#!/usr/bin/env bash

BATCH_LANGS=$@

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"


EFFECTIVE_LANGS=(`echo $BATCH_LANGS $LANGS | tr ' ' '\n' | sort | uniq -d`)
echo "Effectively extracting languages : " $BATCH_LANGS "//" $LANGS "-->" ${EFFECTIVE_LANGS[@]}

[[ x$EFFECTIVE_LANGS == x ]] && exit 0;

git stash -u
git checkout $BITBUCKET_PR_DESTINATION_BRANCH
chmod +x ./kaiko/cicd/*.sh ./kaiko/extractor/*.sh

# Extract data using PR version
DBNARY_DIR="/tmp/$PREVIOUS_VERSION/" "${SCRIPT_DIR}/../extractor/dbnary.sh" -V -Z -n -v "$PREVIOUS_VERSION" -c "$SAMPLE_SIZE" ${EFFECTIVE_LANGS[@]}

mkdir -p target/extracts/$PREVIOUS_VERSION/
cp /tmp/$PREVIOUS_VERSION/extracts/ontolex/latest/*.ttl target/extracts/$PREVIOUS_VERSION/
