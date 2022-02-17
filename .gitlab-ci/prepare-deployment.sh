#!/bin/bash

SSH_DIR=${HOME}/.ssh
mkdir -p $SSH_DIR
chmod 700 $SSH_DIR
cat $IDENTITY > $SSH_DIR/id_rsa
chmod 600 $SSH_DIR/id_rsa
cat $SSH_CONFIG >> $SSH_DIR/config
chmod 644 $SSH_DIR/config
cat $KNOWN_HOSTS >> $SSH_DIR/known_hosts
chmod 600 $SSH_DIR/known_hosts
