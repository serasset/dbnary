#!/bin/bash

## Test if bash version 4 as we need associative arrays.
if [[ $BASH_VERSION != 4.* ]]
then
    echo "Need bash 4 version. Exiting."
    exit -1
fi

## Bootstrapping a virtuoso db.

PREFIX=/home/serasset/dev
if [[ ! $# -eq 0 ]]
then
    PREFIX=$1
fi

VIRTUOSOINITMPL=./virtuoso.ini.tmpl

DBFOLDER=$PREFIX/virtuoso/db.bootstrap
DATASETDIR=$PREFIX/virtuoso/dataset
SERVERPORT=1112
SSLSERVERPORT=2112
WEBSERVERPORT=8899

DBNARYLATEST=/home/serasset/dev/wiktionary/extracts/lemon/latest

declare -A iso3lang
iso3Lang[bg]=bul
iso3Lang[de]=deu
iso3Lang[el]=ell
iso3Lang[en]=eng
iso3Lang[es]=spa
iso3Lang[fi]=fin
iso3Lang[fr]=fra
iso3Lang[it]=ita
iso3Lang[jp]=jpn
iso3Lang[pl]=pol
iso3Lang[pr]=por
iso3Lang[ru]=rus
iso3Lang[tr]=tur


if [ ! -d $DBNARYLATEST ]
then
	echo "latest turtle data not available."
	exit -1
fi

if [ ! -d "$DBFOLDER" ] ; then
	mkdir -p "$DBFOLDER"
	sed "s|@@DBFOLDER@@|$DBFOLDER|g" < $VIRTUOSOINITMPL | \
	sed "s|@@DATASETDIR@@|$DATASETDIR|g" | \
	sed "s|@@SERVERPORT@@|$SERVERPORT|g" | \
	sed "s|@@SSLSERVERPORT@@|$SSLSERVERPORT|g" | \
	sed "s|@@WEBSERVERPORT@@|$WEBSERVERPORT|g" > "$DBFOLDER"/virtuoso.ini
fi

if [ ! -d "$DATASETDIR" ]
then
	mkdir -p "$DATASETDIR"
fi


## Prepare the dataset directory
(
  shopt -s nullglob
  files=($DATASETDIR/*.ttl)
  if [[ "${#files[@]}" -gt 0 ]] ; then
    echo "Dataset already exists and is not empty, assuming its content is up to date."
  else
    echo "Copying and expanding latest extracts."
	cp $DBNARYLATEST/*.ttl.bz2 "$DATASETDIR"
	pushd "$DATASETDIR"
	bunzip2 ./*.ttl.bz2
  fi
)

## create the .graph files for all files in datasetdir
langRegex='(..)_(.*)'
for f in $DATASETDIR/*.ttl
do
    if [[ $f =~ $langRegex ]]
    then
        lg3=${iso3lang[${BASH_REMATCH[1]}]}
        echo "$lg3"
    fi
done


## Launch virtuoso to create the new DB

## connect to isql to load the different configurations

## Change the dba and sparql password

## connect to isql and load all the data

## (TODO: create the virtlabels for correct facetted browsing)

## index facetted browsing

## change .ini file to production settings

## kill production server, move old db folder and substitute by new one, relaunch...