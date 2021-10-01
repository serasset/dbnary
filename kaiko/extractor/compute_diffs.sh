#!/bin/bash

DIR=${DBNARY_DIR:-$HOME/dev/wiktionary/tmp}
VERSION=2.3.6
#LANGS="fr en de pt it fi ru el tr ja es bg pl"
LANGS="fr"
MODEL="ontolex"
PREVIOUS_VERSION=latest.before
NEXT_VERSION=latest.now
DIFFS=diffs

while getopts ":m:v:f:t:d:" opt; do
  case $opt in
    m)
      MODEL=$OPTARG
      ;;
    f)
      PREVIOUS_VERSION=$OPTARG
      ;;
    t)
      NEXT_VERSION=$OPTARG
      ;;
    d)
      DIFFS=$OPTARG
      ;;
    v)
      VERSION="${OPTARG}"
      ;;
    \?)
      echo "Invalid option: -$OPTARG" >&2
      ;;
    :)
      echo "Option -$OPTARG requires an argument." >&2
      exit 1
      ;;
  esac
done
shift $((OPTIND-1))

if [ $# -gt 0 ]
    then
    LANGS=$*
fi

prepareTDBifTooBig() {
  local ttlfile=$1
  if [ -f $1 ]
  then
    fsize=$(wc -c <"$ttlfile")
    if [ $fsize -gt 1000000000 ]
    then
        #More than 1G -> TDB
        >&2 echo "Preparing TDB from big ttl : " ${ttlfile}
        tdbfile=${ttlfile}.tdb
        if [ -d $tdbfile ]
        then
            >&2 echo "TDB directory alrady exists, assuming it's OK: " $tdbfile
            >&2 echo "If not, remove directory and relaunch command."
        else
            >&2 tdbloader2 --loc $tdbfile $ttlfile
        fi
        echo $tdbfile
        return
    fi
  fi
  echo $ttlfile
}

for l in $LANGS
do
    before=$(prepareTDBifTooBig $PREVIOUS_VERSION/${l}_dbnary_${MODEL}*.ttl)
    now=$(prepareTDBifTooBig $NEXT_VERSION/${l}_dbnary_${MODEL}*.ttl)
    >&2 echo Comparing ${before} and ${now}
  java -Xmx16G -cp ${HOME}/.m2/repository/org/getalp/dbnary-extractor/$VERSION/dbnary-extractor-$VERSION-jar-with-dependencies.jar \
    org.getalp.dbnary.cli.RDFDiff ${before} ${now} > $DIFFS/${l}_lost_${MODEL}.ttl ;
  java -Xmx16G -cp ${HOME}/.m2/repository/org/getalp/dbnary-extractor/$VERSION/dbnary-extractor-$VERSION-jar-with-dependencies.jar \
    org.getalp.dbnary.cli.RDFDiff ${now} ${before} > $DIFFS/${l}_gain_${MODEL}.ttl ;
done
