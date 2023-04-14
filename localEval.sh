#!/usr/bin/env bash

## Pass options to dbnary commands in DBNARY_OPTS
## Pass list of models in EVAL_MODELS
## e.g. DBNARY_OPTS="--endolex=morphology" EVAL_MODELS="endolex morphology" ./localEval.sh en
verbose=0
origin=git@gitlab.com:gilles.serasset/dbnary.git
versus=develop
passlocalupdate=0
passversusupdate=0

while getopts "o:v:VLDd:" opt; do
  case $opt in
  V)
    verbose=1
    ;;
  v)
    versus="$OPTARG"
    ;;
  o)
    origin="$OPTARG"
    ;;
  L)
    passlocalupdate=1
    ;;
  D)
    passversusupdate=1
    ;;
  \?)
    echo "Invalid option: -$OPTARG" >&2
    exit 0
    ;;
  :)
    echo "Option -$OPTARG requires an argument." >&2
    exit 1
    ;;
  esac
done

shift $((OPTIND - 1))

set -e
set -v

NEXT_DIR="target/evaluation/ci-next-version"
PREVIOUS_DIR="target/evaluation/ci-previous-version"
SAMPLE_SIZE=10000
DUMPS=${HOME}/dev/wiktionary/dumps

if [[ $passlocalupdate -eq 0 ]]; then
  mkdir -p ${NEXT_DIR}
  pushd ${NEXT_DIR}
  [[ -L dumps ]] || ln -s "${DUMPS}" .
  popd

  if [ "$verbose" = 1 ]; then
    echo "Compiling local version"
  fi
  mvn install
  if [ "$verbose" = 1 ]; then
    echo "Extracting samples with local version"
  fi
  echo dbnary-commands/target/appassembler/bin/dbnary update --dir "${NEXT_DIR}" -v --no-compress --sample "$SAMPLE_SIZE" ${DBNARY_OPTS} "$@"
fi #$passlocalupdate
##### PREVIOUS VERSION

mkdir -p ${PREVIOUS_DIR}
pushd ${PREVIOUS_DIR}
[[ -L dumps ]] || ln -s "${DUMPS}" .
popd

if [[ $passversusupdate -eq 0 ]]; then
  if [ "$verbose" = 1 ]; then
    echo "Cloning previous version"
  fi
  mkdir -p target/versus
  pushd target/versus
  [[ -d dbnary ]] || git clone "$origin"
  pushd dbnary
  git checkout "$versus"

  if [ "$verbose" = 1 ]; then
    echo "Compiling previous version"
  fi
  mvn clean install
  popd
  popd
  if [ "$verbose" = 1 ]; then
    echo "Extracting samples with local version"
  fi
  target/versus/dbnary/dbnary-commands/target/appassembler/bin/dbnary update --dir ${PREVIOUS_DIR} -v --no-compress --sample "$SAMPLE_SIZE"  ${DBNARY_OPTS} "$@"
fi
[[ -d target/diffs ]] || mkdir -p target/diffs
for model in ${EVAL_MODELS}; do
  kaiko/extractor/compute_diffs.sh -m "$model" -f "${PREVIOUS_DIR}/extracts/ontolex/latest/" -t "${NEXT_DIR}/extracts/ontolex/latest/" -d "target/diffs" -x "dbnary-commands/target/appassembler/bin/rdfdiff" "$@"
done