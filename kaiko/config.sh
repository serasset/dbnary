## Virtuoso and data configuration variables
## this file will be sourced by bootstrapping scripts,
## where the PREFIX variable should be set.
#
## Bootstrapping a virtuoso db.

if [[ z$PREFIX == z ]]
then
    echo the PREFIX variable should be set before sourcing the configuration file
    exit 1;
fi

VIRTUOSOINITMPL=./virtuoso.ini.tmpl
BOOTSTRAPSQL=./bootstrap.sql

DBFOLDER=$PREFIX/virtuoso/db.bootstrap
EMPTYDBFOLDER=$PREFIX/virtuoso/db.empty
DATASETDIR=$PREFIX/virtuoso/dataset
SERVERPORT=1112
SSLSERVERPORT=2112
WEBSERVERPORT=8899

# Virtuoso installation variables
#PATH=/sbin:/bin:/usr/sbin:/usr/bin:/opt/virtuoso-opensource/bin
PATH=/sbin:/bin:/usr/sbin:/usr/bin:/opt/virtuoso-opensource7/bin
#DAEMON=/opt/virtuoso-opensource/bin/virtuoso-t
DAEMON=/opt/virtuoso-opensource7/bin/virtuoso-t
NAME=virtuoso