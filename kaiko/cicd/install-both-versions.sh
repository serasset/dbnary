#!/bin/bash

if [ x$BITBUCKET_PR_DESTINATION_BRANCH != x ]; then
    mvn versions:set -B -DnewVersion=ci-next-version
    mvn install
    git checkout $BITBUCKET_PR_DESTINATION_BRANCH
    mvn versions:set -B -DnewVersion=ci-previous-version
    mvn install
else
    exit 1
fi

