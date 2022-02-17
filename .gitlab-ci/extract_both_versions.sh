#!/usr/bin/env bash

BATCH_LANGS=$@

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "==== PREPARING ENVIRONMENT ==== "

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

EFFECTIVE_LANGS=(`echo $BATCH_LANGS $LANGS | tr ' ' '\n' | sort | uniq -d`)
echo "Effectively extracting languages : " $BATCH_LANGS "//" $LANGS "-->" ${EFFECTIVE_LANGS[@]}

[[ x$EFFECTIVE_LANGS == x ]] && exit 0;

set -x

# Prepare directory layout
mkdir -p "target/evaluation/$NEXT_VERSION/"
mkdir -p "target/evaluation/$PREVIOUS_VERSION/"
mkdir -p "$DIFFS"

# Share the dumps directory between both versions to avoid reloading the dumps for each version
ln -s "/dumps" "target/evaluation/$NEXT_VERSION/dumps"
ln -s "/dumps" "target/evaluation/$PREVIOUS_VERSION/dumps"
ls -al "target/evaluation/$NEXT_VERSION/*" "target/evaluation/$PREVIOUS_VERSION/*"

echo " ==== EXTRACTING WITH NEXT VERSION ===== "
FEATURES="--endolex=ontolex,morphology,lime,etymology,enhancement,statistics --exolex=ontolex"
# Extract data using Target branch version
"$BINDIR/$NEXT_VERSION/dbnary-commands-${NEXT_VERSION}/bin/dbnary" update --dir "target/evaluation/$NEXT_VERSION/" -v --no-compress --no-network $FEATURES --sample "$SAMPLE_SIZE" ${EFFECTIVE_LANGS[@]}

echo " ==== EXTRACTING WITH PREVIOUS VERSION ===== "

# Extract data using PR version
"$BINDIR/$PREVIOUS_VERSION/dbnary-commands-${PREVIOUS_VERSION}/bin/dbnary" update --dir "target/evaluation/$PREVIOUS_VERSION/" --no-compress --no-network $FEATURES --sample "$SAMPLE_SIZE" ${EFFECTIVE_LANGS[@]}
