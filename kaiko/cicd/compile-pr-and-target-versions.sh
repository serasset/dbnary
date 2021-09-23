#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source ${SCRIPT_DIR}/settings

# Compile PR and DESTINATION versions
if [ x$BITBUCKET_PR_DESTINATION_BRANCH != x ]; then
    mvn versions:set -B -DnewVersion=$NEXT_VERSION
    mvn install
    git checkout $BITBUCKET_PR_DESTINATION_BRANCH
    mvn versions:set -B -DnewVersion=$PREVIOUS_VERSION
    mvn install
else
    echo "No Pull Request destination branch, is this really a Pull Request ?"
    exit 1
fi

# Prepare directory layout
mkdir -p /tmp/$NEXT_VERSION/
mkdir -p /tmp/$PREVIOUS_VERSION/
mkdir -p $DIFFS

# Share the dumps directory between both versions to avoid reloading the dumps for each version
mkdir -p /tmp/$PREVIOUS_VERSION/dumps
ln -s /tmp/$NEXT_VERSION/dumps ../$PREVIOUS_VERSION/dumps

ls -al /tmp/$NEXT_VERSION /tmp/$PREVIOUS_VERSION

