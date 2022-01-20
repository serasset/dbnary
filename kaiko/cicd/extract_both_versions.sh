#!/usr/bin/env bash

BATCH_LANGS=$@

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "==== PREPARING ENVIRONMENT ==== "
chmod +x ./kaiko/cicd/*.sh ./kaiko/extractor/*.sh

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

EFFECTIVE_LANGS=(`echo $BATCH_LANGS $LANGS | tr ' ' '\n' | sort | uniq -d`)
echo "Effectively extracting languages : " $BATCH_LANGS "//" $LANGS "-->" ${EFFECTIVE_LANGS[@]}

[[ x$EFFECTIVE_LANGS == x ]] && exit 0;

# Cleanup maven repo to remove dbnary artifacts that may be downloaded from cached.
mkdir -p ${HOME}/.m2/repository/org/
rm -rf ${HOME}/.m2/repository/org/getalp
cp -r target/dbnary-maven-repository/org/getalp ${HOME}/.m2/repository/org/

# Prepare for proper SSH connection to the dumps host.
mkdir -p ~/.ssh
cat ./kaiko/cicd/my_known_hosts >> ~/.ssh/known_hosts

set -x

# Prepare directory layout
mkdir -p "/tmp/$NEXT_VERSION/"
mkdir -p "/tmp/$PREVIOUS_VERSION/"
mkdir -p "$DIFFS"

# Share the dumps directory between both versions to avoid reloading the dumps for each version
mkdir -p "/tmp/$PREVIOUS_VERSION/dumps"
ln -s "../$PREVIOUS_VERSION/dumps" "/tmp/$NEXT_VERSION/dumps"
ls -al "/tmp/$NEXT_VERSION" "/tmp/$PREVIOUS_VERSION"

echo "==== FETCHING WIKTIONARY DUMPS ==== "
# Fetching all uncompressed dumps directories from kopi
for lg in $LANGS;
do
  echo "Fetching uncompressed dumps for $lg"
  # This will be usable (need tests) when I will use an image that contains rsync (not the case of the usual maven image.
  #   rsync -a --include='*.idx' --include='*.xml' --include='*/' --exclude='*' "${WIKTIONARY_DUMPS_USER}@${WIKTIONARY_DUMPS_HOST}:${WIKTIONARY_DUMPS_DIR}/${lg}" "/tmp/$PREVIOUS_VERSION/dumps/"
  scp -r "${WIKTIONARY_DUMPS_USER}@${WIKTIONARY_DUMPS_HOST}:${WIKTIONARY_DUMPS_DIR}/${lg}" "/tmp/$PREVIOUS_VERSION/dumps/"
  # df -h
  # ls -al "/tmp/$PREVIOUS_VERSION/dumps/${lg}"
  # ls -al "/tmp/$NEXT_VERSION/dumps/${lg}"
done

# ls -al "/tmp/$NEXT_VERSION/"
# ls -al "/tmp/$NEXT_VERSION/dumps/"
# ls -al "/tmp/$NEXT_VERSION/dumps"/*
# ls -al "/tmp/$NEXT_VERSION/dumps"/*/*

echo " ==== EXTRACTING WITH NEXT VERSION ===== "
# Extract data using Target branch version
DBNARY_DIR="/tmp/$NEXT_VERSION/" "${SCRIPT_DIR}/../extractor/dbnary.sh" -V -Z -n -v "$NEXT_VERSION" -c "$SAMPLE_SIZE" ${EFFECTIVE_LANGS[@]}

mkdir -p target/extracts/$NEXT_VERSION/
cp /tmp/$NEXT_VERSION/extracts/ontolex/latest/*.ttl target/extracts/$NEXT_VERSION/

echo " ==== EXTRACTING WITH PREVIOUS VERSION ===== "
git stash -u
git checkout $BITBUCKET_PR_DESTINATION_BRANCH
chmod +x ./kaiko/cicd/*.sh ./kaiko/extractor/*.sh

# Extract data using PR version
DBNARY_DIR="/tmp/$PREVIOUS_VERSION/" "${SCRIPT_DIR}/../extractor/dbnary.sh" -V -Z -n -v "$PREVIOUS_VERSION" -c "$SAMPLE_SIZE" ${EFFECTIVE_LANGS[@]}

mkdir -p target/extracts/$PREVIOUS_VERSION/
cp /tmp/$PREVIOUS_VERSION/extracts/ontolex/latest/*.ttl target/extracts/$PREVIOUS_VERSION/
