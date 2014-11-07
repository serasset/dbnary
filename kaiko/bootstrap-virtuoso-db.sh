#!/bin/bash

## Bootstrapping a virtuoso db.

VIRTUOSOINITMPL=./virtuoso.ini.tmpl

DBFOLDER=/home/serasset/dev/virtuoso/db.bootstrap
DATASETDIR=/home/serasset/dev/virtuoso/dataset
SERVERPORT=1112
SSLSERVERPORT=2112
WEBSERVERPORT=8899

DBNARYLATEST=/home/serasset/dev/wiktionary/extracts/lemon/latest

if [ ! -d $DBNARYLATEST ]
then
	echo "latest turtle data not available."
	exit -1
fi

if [ ! -d $DBFOLDER ] ; then
	mkdir -p $DBFOLDER
	sed "s|@@DBFOLDER@@|$DBFOLDER|g" < $VIRTUOSOINITMPL | \
	sed "s|@@DATASETDIR@@|$DATASETDIR|g" | \
	sed "s|@@SERVERPORT@@|$SERVERPORT|g" | \
	sed "s|@@SSLSERVERPORT@@|$SSLSERVERPORT|g" | \
	sed "s|@@WEBSERVERPORT@@|$WEBSERVERPORT|g" > $DBFOLDER/virtuoso.ini
fi

if [ ! -d $DATASETDIR ]
then
	mkdir -p $DATASETDIR
fi

(
  shopt -s nullglob
  files=($DATASETDIR/*.ttl)
  if [[ "${#files[@]}" -gt 0 ]] ; then
    echo "Dataset already exists and is not empty, assuming its content is up to date."
  else
    echo "Copying and expanding latest extracts."
	cp $DBNARYLATEST/*.ttl.bz2 $DATASETDIR
	pushd $DATASETDIR
	bunzip2 *.ttl.bz2
  fi
)

