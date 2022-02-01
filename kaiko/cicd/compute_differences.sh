#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

mkdir -p $DIFFS

for model in ontolex morphology etymology enhancement
do
  # compute differences
  echo "${SCRIPT_DIR}/../extractor/compute_diffs.sh -v $NEXT_VERSION -m $model -f target/extracts/$PREVIOUS_VERSION/ -t target/extracts/$NEXT_VERSION/ -d $DIFFS $LANGS"
  PATH=$PATH:output/dbnary/$NEXT_VERSION/bin "${SCRIPT_DIR}/../extractor/compute_diffs.sh" -v "$NEXT_VERSION" -m $model -f "target/extracts/$PREVIOUS_VERSION/" -t "target/extracts/$NEXT_VERSION/" -d "$DIFFS" $LANGS
done
