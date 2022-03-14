#!/usr/bin/env bash

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

echo "Sourcing settings from: ${SCRIPT_DIR}/settings"
source "${SCRIPT_DIR}/settings"

echo "Bash Version : ${BASH_VERSION}"
echo "Commit Message = $CI_COMMIT_MESSAGE"
echo "Validating on languages : $LANGS"
echo "Current Branch : $NEXT_VERSION_BRANCH"
echo "Pull request destination branch : $PREVIOUS_VERSION_BRANCH"

set -x

# make sure folder is in gitignore to avoid losing the packaged app after git stash and mvn clean
## Ensure there is an EOL at the end of the gitignore file.
echo >> .gitignore
echo out >> .gitignore
echo .m2 >> .gitignore

mkdir -p "$BINDIR/$NEXT_VERSION"
mvn versions:set -B -DnewVersion="$NEXT_VERSION"
mvn package
cp -r dbnary-commands/target/distributions/dbnary/*.tar.gz "$BINDIR/$NEXT_VERSION"

ls -al
ls -al out/dbnary/ci-next-version
tail .gitignore

mvn clean

ls -al
ls -al out/dbnary/ci-next-version

tail .gitignore

git stash -u
git checkout "$PREVIOUS_VERSION_BRANCH"

## Ensure there is an EOL at the end of the gitignore file.
echo >> .gitignore
echo out >> .gitignore
echo .m2 >> .gitignore

ls -al
ls -al out/dbnary/ci-next-version
tail .gitignore

mkdir -p "$BINDIR/$PREVIOUS_VERSION"
mvn versions:set -B -DnewVersion="$PREVIOUS_VERSION"
mvn package
cp -r dbnary-commands/target/distributions/dbnary/*.tar.gz "$BINDIR/$PREVIOUS_VERSION"
mvn clean

# Then, switch back to latest branch so that latest improvement in CI/CD are used.
git stash -u
git checkout "$NEXT_VERSION_BRANCH"

pushd "$BINDIR/$PREVIOUS_VERSION"
tar zxvf ./*.tar.gz
rm -- *.tar.gz
popd
pushd "$BINDIR/$NEXT_VERSION"
tar zxvf -- ./*.tar.gz
rm -- *.tar.gz
popd