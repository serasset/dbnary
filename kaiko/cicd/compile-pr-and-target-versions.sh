#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source ${SCRIPT_DIR}/settings
set -x

echo "Commit Message = $COMMIT_MESSAGE"
echo "Validating on languages : $LANGS"

# Compile PR and DESTINATION versions
if [ x$BITBUCKET_PR_DESTINATION_BRANCH == x ]; then
  echo "Not a Pull Request, I will compare branch with develop"
  BITBUCKET_PR_DESTINATION_BRANCH=develop
  if [ x$BITBUCKET_BRANCH == x -o $BITBUCKET_BRANCH == develop ]; then
    echo "Source branch undefined or already develop."
    exit 1
  fi
fi

mvn versions:set -B -DnewVersion=$NEXT_VERSION
mvn install
git stash -u
git checkout $BITBUCKET_PR_DESTINATION_BRANCH
mvn versions:set -B -DnewVersion=$PREVIOUS_VERSION
mvn install