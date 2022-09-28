#!/bin/bash

git clone --branch="develop" https://gitlab.com/gilles.serasset/dbnary.git
cd dbnary
chmod +x ./kaiko/cicd/*.sh ./kaiko/extractor/*.sh
./kaiko/cicd/compile-pr-and-target-versions.sh
git stash -u
git checkout develop
chmod +x ./kaiko/cicd/*.sh ./kaiko/extractor/*.sh
