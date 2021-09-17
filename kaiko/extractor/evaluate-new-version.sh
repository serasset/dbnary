#!/bin/bash

# the directory of the script
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# the temp directory used, within $DIR
# omit the -p parameter to create a temporal directory in the default location
WORK_DIR=`mktemp -d`

# check if tmp dir was created
if [[ ! "$WORK_DIR" || ! -d "$WORK_DIR" ]]; then
  echo "Could not create temp dir"
  exit 1
fi

# deletes the temp directory
function cleanup {
  rm -rf "$WORK_DIR"
  echo "Deleted temp working directory $WORK_DIR"
}

# register the cleanup function to be called on the EXIT signal
trap cleanup EXIT

mkdir ${DIFF_DIR:=$WORK_DIR/diffs}
mkdir ${STABLE_EXTRACT_DIR:=$WORK_DIR/extracts.stable}
mkdir ${CURRENT_EXTRACT_DIR:=$WORK_DIR/extracts.current}

# compute current extract
DBNARY_DIR=WORK_DIR $DIR/dbnary.sh bg
cp $WORK_DIR/extracts/ontolex/latest/${LANG}_dbnary_*.ttl.bz2 $CURRENT_EXTRACT_DIR/
bunzip2 $CURRENT_EXTRACT_DIR/*.bz2

#TODO : compute the stable version (using latest version number ???) as we want to get only a sample of the extracts

DBNARY_DIR=WORK_DIR $DIR/compute_diffs.sh bg
