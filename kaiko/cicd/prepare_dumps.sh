#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

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

# Fetching all uncompressed dumps directories from kopi
for lg in $LANGS;
do
  echo "Fetching uncompressed dumps for $lg"
  rsync -a --include='*.idx' --include='*.xml' --include='*/' --exclude='*' "${WIKTIONARY_DUMPS_USER}@${WIKTIONARY_DUMPS_HOST}:${WIKTIONARY_DUMPS_DIR}/${lg}" "/tmp/$PREVIOUS_VERSION/dumps/"
  df -h
  ls -al "/tmp/$PREVIOUS_VERSION/dumps/${lg}"
  ls -al "/tmp/$NEXT_VERSION/dumps/${lg}"
done

ls -al "/tmp/$NEXT_VERSION/"
ls -al "/tmp/$NEXT_VERSION/dumps/"
ls -al "/tmp/$NEXT_VERSION/dumps"/*
ls -al "/tmp/$NEXT_VERSION/dumps"/*/*

