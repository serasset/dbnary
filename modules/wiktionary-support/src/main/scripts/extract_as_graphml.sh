#!/bin/bash

wktextractjar="wiktionary-support-1.0-SNAPSHOT-jar-with-dependencies.jar"
wktextractprefix="$HOME/dev/blexisma/modules/wiktionary-support/target"
languages="fr"	# default languages
dumpfolder="$HOME/dev/wiktionary/latest"
outfolder="$HOME/dev/wiktionary/extracts"
stamp=`date +%Y%m%d%k%M`
force=0

while getopts j:p:d:o:s:f o
do	case "$o" in
	j)	wktextractjar="$OPTARG";;
	p)	wktextractprefix="$OPTARGS";;
	d)	dumpfolder="$OPTARG";;
	o)  outfolder="$OPTARG";;
	s)	stamp="$OPTARGS";;
	f)	force=1;;
	[?])	print >&2 "Usage: $0 [-j wiktionary-support-jar] [-p wiktionary-support-prefix] [-d dumpfolder] [-o outputfolder] [-s stamp] [-f] [lg ...]"
		exit 1;;
	esac
done
shift `expr $OPTIND - 1`

if [ $# -ne 0 ]
then
  languages=$@
fi

if [ ! -d $outfolder ]
then
  mkdir -p $outfolder
fi

if [ ! -d $dumpfolder ]
then
  echo Dump folder: $dumpfolder does not exist.
  exit 1
fi

if [ -d $outfolder/$stamp -a $force -eq 0 ]
then
  echo Output folder already exist, use -f to force execution
  exit 1
else
  mkdir -p $outfolder/$stamp
fi

cd $outfolder/$stamp

for lg in $languages
do
  dumpfile="$dumpfolder/${lg}wkt.xml"
  wfile="${lg}_extract.graphml"
  if [ -f ./${wfile} ]
  then
    rm -f ./${wfile}
  fi
  cmd="java -Xmx2G -Dfile.encoding=UTF-8 -cp ${wktextractprefix}/${wktextractjar} org.getalp.blexisma.wiktionary.cli.ExtractWiktionary -l ${lg} -o ./${wfile} -f graphml ${dumpfile}"
  echo Extracting data from ${lg} dump.
  echo $cmd
  $cmd
done