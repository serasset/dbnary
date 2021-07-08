#!/bin/sh

DIR=${HOME}/dev/wiktionary/tmp
VERSION=2.3.5-SNAPSHOT
#LANGS="fr en de pt it fi ru el tr ja es bg pl"
LANGS="fr"

DEBUG=""
DATE="20210101"
VERBOSE=""

while getopts ":d:t:v:D:nmMeElLsSTVxX" opt; do
  case $opt in
  d)
    DEBUG="${DEBUG} -Dorg.slf4j.simpleLogger.log.${OPTARG}=debug"
    ;;
  t)
    DEBUG="${DEBUG} -Dorg.slf4j.simpleLogger.log.${OPTARG}=trace"
    ;;
  v)
    VERSION="${OPTARG}"
    ;;
  D)
    DATE="${OPTARG}"
    ;;
  V)
    VERBOSE="-v"
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
shift $((OPTIND - 1))

if [ $# -gt 0 ]; then
  LANGS=$*
fi

TLANGS="fra,eng,por,deu,ell,rus,ita,fin,tur,jpn"
JAVA=java

#-Dorg.slf4j.simpleLogger.log.info.bliki.extensions.scribunto.engine.lua=trace
#-Dorg.slf4j.simpleLogger.log.org.getalp.dbnary=trace
#-Dorg.slf4j.simpleLogger.log.org.getalp.dbnary.OntolexBasedRDFDataHandler

if [ ! -z $VERBOSE ]; then
  echo "$JAVA -Xmx16g -Djava.net.useSystemProxies=true ${DEBUG} \
  -cp ${HOME}/.m2/repository/org/getalp/dbnary-extractor/$VERSION/dbnary-extractor-$VERSION-jar-with-dependencies.jar \
    org.getalp.dbnary.cli.CheckWiktionarySyntaxQuality $VERBOSE $DATE $lg 2> $lg.err.txt"
fi

for lg in $LANGS; do
  $JAVA -Xmx16g -Djava.net.useSystemProxies=true ${DEBUG} \
    -cp ${HOME}/.m2/repository/org/getalp/dbnary-extractor/$VERSION/dbnary-extractor-$VERSION-jar-with-dependencies.jar \
    org.getalp.dbnary.cli.CheckWiktionarySyntaxQuality ${VERBOSE} dumps/${lg}/${DATE}/${lg}wkt-${DATE}.xml 2>$lg.err.txt
done
