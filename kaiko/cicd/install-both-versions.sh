#!/bin/bash

if [ x$BITBUCKET_PR_DESTINATION_BRANCH != x ]; then
    mvn versions:set -B -DnewVersion=ci-next-version
    mvn install
    git checkout $BITBUCKET_PR_DESTINATION_BRANCH
    mvn versions:set -B -DnewVersion=ci-previous-version
    mvn install
else
    echo "No Pull Request destination branch, is this really a Pull Request ?"
    exit 1
fi

