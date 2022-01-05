#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

# Make sure the scripts are the correct ones
# Compile PR and DESTINATION versions
if [ "x$BITBUCKET_PR_DESTINATION_BRANCH" == "x" ]; then
  if [[ "$BITBUCKET_BRANCH" =~ "^feature/.*$" ]]; then
    BITBUCKET_PR_DESTINATION_BRANCH=develop
  else
    BITBUCKET_PR_DESTINATION_BRANCH=master
  fi
  echo "Not a Pull Request, I will compare branch with $BITBUCKET_PR_DESTINATION_BRANCH"
fi

pwd
git branch
git checkout "$BITBUCKET_PR_DESTINATION_BRANCH" -- kaiko
git checkout master
echo "=========="

# Extract data using PR version
DBNARY_DIR="/tmp/$PREVIOUS_VERSION/" "${SCRIPT_DIR}/../extractor/dbnary.sh" -V -Z -n -v "$PREVIOUS_VERSION" -c "$SAMPLE_SIZE" $LANGS

mkdir -p target/extracts/$PREVIOUS_VERSION/
cp /tmp/$PREVIOUS_VERSION/extracts/ontolex/latest/*.ttl target/extracts/$PREVIOUS_VERSION/
