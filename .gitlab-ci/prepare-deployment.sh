#!/bin/bash

mkdir -p .ssh
chmod 700 .ssh
cat $IDENTITY > .ssh/id_rsa
chmod 600 .ssh/id_rsa
cat $SSH_CONFIG >> .ssh/config
chmod 644 .ssh/config
cat $KNOWN_HOSTS >> .ssh/known_hosts
chmod 600 .ssh/known_hosts
