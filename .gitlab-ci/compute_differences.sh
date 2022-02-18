#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

mkdir -p $DIFFS

for model in ontolex morphology etymology enhancement exolex_ontolex
do
  # compute differences
  echo "${CI_PROJECT_DIR}/kaiko/extractor/compute_diffs.sh -m $model -f target/evaluation/$PREVIOUS_VERSION/ -t target/evaluation/$NEXT_VERSION/ -d $DIFFS -x $BINDIR/$NEXT_VERSION/bin/rdfdiff $LANGS"
  "${CI_PROJECT_DIR}/kaiko/extractor/compute_diffs.sh" -m $model -f "target/evaluation/$PREVIOUS_VERSION/" -t "target/evaluation/$NEXT_VERSION/" -d "$DIFFS" -x "$BINDIR/$NEXT_VERSION/bin/rdfdiff" $LANGS
done
