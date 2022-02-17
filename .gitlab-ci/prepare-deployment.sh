#!/bin/bash

SSH_DIR=${HOME}/.ssh
mkdir -p $SSH_DIR
chmod 700 $SSH_DIR
cat $IDENTITY > $SSH_DIR/id_rsa
chmod 600 $SSH_DIR/id_rsa
cat $KNOWN_HOSTS >> $SSH_DIR/known_hosts
chmod 600 $SSH_DIR/known_hosts
