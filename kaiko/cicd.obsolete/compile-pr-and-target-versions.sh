#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

echo "Bash Version : ${BASH_VERSION}"
echo "Commit Message = $COMMIT_MESSAGE"
echo "Validating on languages : $LANGS"
echo "Current Branch : $BITBUCKET_BRANCH"
echo "Pull request destination branch : $BITBUCKET_PR_DESTINATION_BRANCH"

# Compile PR and DESTINATION versions
if [ "x$BITBUCKET_PR_DESTINATION_BRANCH" == "x" ]; then
  if [[ "$BITBUCKET_BRANCH" =~ "^feature/" ]]; then
    BITBUCKET_PR_DESTINATION_BRANCH=develop
  else
    BITBUCKET_PR_DESTINATION_BRANCH=master
  fi
  echo "Not a Pull Request, I will compare branch with $BITBUCKET_PR_DESTINATION_BRANCH"
  if [ "x$BITBUCKET_BRANCH" == "x" -o "$BITBUCKET_BRANCH" == "$BITBUCKET_PR_DESTINATION_BRANCH" ]; then
    echo "Source branch and target branches are the same."
    exit 1
  fi
fi

set -x

# make sure folder is in gitignore to avoid losing the packaged app after git stash and mvn clean
echo out >> .gitignore
mkdir -p $BINDIR
mvn versions:set -B -DnewVersion="$NEXT_VERSION"
mvn package
cp -r dbnary-commands/target/appassembler $BINDIR/$NEXT_VERSION
mvn clean

git stash -u
git checkout "$BITBUCKET_PR_DESTINATION_BRANCH"
echo out >> .gitignore

mvn versions:set -B -DnewVersion="$PREVIOUS_VERSION"
mvn package
mkdir -p $BINDIR
cp -r dbnary-commands/target/appassembler $BINDIR/$PREVIOUS_VERSION
mvn clean

# Then, switch back to latest branch so that latest improvement in CI/CD are used.
git stash -u
git checkout "$BITBUCKET_BRANCH"