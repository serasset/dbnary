#!/bin/bash

SAMPLE_SIZE=10000
LANGS="fr en de"
#LANGS="fr en de pt it fi ru el tr ja es bg pl nl sh sv lt no mg id la ku"
PREVIOUS_VERSION=ci-previous-version
NEXT_VERSION=ci-next-version
DIFFS=/tmp/diffs

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
mkdir -p /tmp/$NEXT_VERSION-extracts/
mkdir -p /tmp/$PREVIOUS_VERSION-extracts/
mkdir -p $DIFFS



# Extract data using PR version
DBNARY_DIR=/tmp/$NEXT_VERSION/ dbnary.sh -V -v $NEXT_VERSION -c $SAMPLE_SIZE $LANGS

# Extract data using Target branch version
DBNARY_DIR=/tmp/$PREVIOUS_VERSION/ dbnary.sh -V -v $PREVIOUS_VERSION -c $SAMPLE_SIZE $LANGS

# compute differences
compute_diff.sh -v $PREVIOUS_VERSION -f /tmp/$PREVIOUS_VERSION -t /tmp/$NEXT_VERSION -d $DIFFS $LANGS

# Cleanup maven repo to remove dbnary artifacts that should not be cached.
rm -rf ${HOME}/.m2/repository/org/getalp/*