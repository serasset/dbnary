#!/bin/bash

# set -e

# move custom config to FUSEKI_BASE (don't overwrite)
mkdir -p $FUSEKI_BASE/config \
  && mkdir -p $FUSEKI_BASE/configuration
cp -nr $FUSEKI_ROOT/config/datasets/*.ttl $FUSEKI_BASE/configuration/
cp -nr $FUSEKI_ROOT/config/config.ttl $FUSEKI_BASE/config.ttl
rm -rf $FUSEKI_ROOT/databases
ln -s $FUSEKI_BASE/databases $FUSEKI_ROOT/databases

# If GOSU_CHOWN environment variable set, recursively chown all specified directories
# to match the user:group set in GOSU_USER environment variable.
if [ -n "$GOSU_CHOWN" ]; then
    for DIR in $GOSU_CHOWN
    do
        chown -R $GOSU_USER $DIR
    done
fi

FUSEKI_OPTS=${FUSEKI_OPTS:-"--config $FUSEKI_BASE/config.ttl"}
cd $FUSEKI_ROOT
# If GOSU_USER environment variable set to something other than 0:0 (root:root),
# become user:group set within and exec command passed in args
if [ "$GOSU_USER" != "0:0" ]; then
    exec gosu $GOSU_USER $FUSEKI_ROOT/fuseki-server $FUSEKI_OPTS "$@"
fi

# If GOSU_USER was 0:0 exec command passed in args without gosu (assume already root)

exec "$@"