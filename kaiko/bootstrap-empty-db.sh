#!/bin/bash

## Test if bash version 4 as we need associative arrays.
if [[ $BASH_VERSION != 4.* ]]
then
    echo "Need bash 4 version. Exiting."
    exit -1
fi

## Bootstrapping a virtuoso db.

PREFIX=$HOME/develop
if [[ ! $# -eq 0 ]]
then
    PREFIX=$1
fi

DBNARY_GLOBAL_CONFIG="$HOME/.dbnary/config"
[[ -f $DBNARY_GLOBAL_CONFIG ]] && source $DBNARY_GLOBAL_CONFIG
[[ -f ./config ]] && source ./config

#source config.sh

test -x $DAEMON || (echo "Could not find virtuoso-t bin" && exit 0)

if [ ! -d "$EMPTYDBFOLDER" ] ; then
    mkdir -p "$EMPTYDBFOLDER"
    sed "s|@@DBFOLDER@@|$EMPTYDBFOLDER|g" < $VIRTUOSOINITMPL | \
    sed "s|@@DATASETDIR@@|$DATASETDIR|g" | \
    sed "s|@@SERVERPORT@@|$SERVERPORT|g" | \
    sed "s|@@SSLSERVERPORT@@|$SSLSERVERPORT|g" | \
    sed "s|@@WEBSERVERPORT@@|$WEBSERVERPORT|g" > "$EMPTYDBFOLDER"/virtuoso.ini
    cp $BOOTSTRAPSQL "$EMPTYDBFOLDER"
elif [[ -f $EMPTYDBFOLDER/virtuoso.db ]]; then
    echo "Virtuoso database file already exists, please clean up the db.empty dir."
    exit -1
fi

## Launch virtuoso to create the new DB
echo "Launching daemon."
pushd "$EMPTYDBFOLDER" || exit -1
$DAEMON -c $NAME +wait &
daemon_pid=$!
wait
### RECUPERER LE BON PID...

# exit 0

## connect to isql to load the different configurations
isql $SERVERPORT dba dba $BOOTSTRAPSQL

## Now change admin passwords and shutdown the database.
IFS= read -s  -p Password: pwd

isql $SERVERPORT dba dba <<END
user_change_password('dba','dba', '$pwd');
user_change_password('dav','dav', '$pwd');
shutdown();
END

## change .ini file to production settings

## kill production server, move old db folder and substitute by new one, relaunch...
