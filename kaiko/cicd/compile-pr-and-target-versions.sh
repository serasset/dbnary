#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source ${SCRIPT_DIR}/settings
set -x

# Compile PR and DESTINATION versions
if [ x$BITBUCKET_PR_DESTINATION_BRANCH != x ]; then
    mvn versions:set -B -DnewVersion=$NEXT_VERSION
    mvn install
    git stash -u
    git checkout $BITBUCKET_PR_DESTINATION_BRANCH
    mvn versions:set -B -DnewVersion=$PREVIOUS_VERSION
    mvn install
else
    echo "No Pull Request destination branch, is this really a Pull Request ?"
    exit 1
fi