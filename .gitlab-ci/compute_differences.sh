#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

mkdir -p "$DIFFS"

echo Checking environment
ls -al /builds/gilles.serasset/dbnary/out/dbnary/ci-next-version/
ls -al $NEXT_VERSION_BIN/

for model in ontolex morphology etymology enhancement exolex_ontolex
do
  # compute differences
  echo "${CI_PROJECT_DIR}/kaiko/extractor/compute_diffs.sh -m $model -f target/evaluation/$PREVIOUS_VERSION/extracts/ontolex/latest/ -t target/evaluation/$NEXT_VERSION/extracts/ontolex/latest/ -d $DIFFS -x $NEXT_VERSION_BIN/rdfdiff $LANGS"
  # shellcheck disable=SC2086
  "${CI_PROJECT_DIR}/kaiko/extractor/compute_diffs.sh" -m $model -f "target/evaluation/$PREVIOUS_VERSION/extracts/ontolex/latest/" -t "target/evaluation/$NEXT_VERSION/extracts/ontolex/latest/" -d "$DIFFS" -x "$NEXT_VERSION_BIN/rdfdiff" $LANGS
done
