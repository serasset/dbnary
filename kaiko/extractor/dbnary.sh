#!/bin/sh

DIR=${HOME}/dev/wiktionary/tmp
VERSION=2.2.3-SNAPSHOT
#LANGS="fr en de pt it fi ru el tr ja es bg pl"
LANGS="fr"

DEBUG=""
NETWORK=""
MORPHO="--enable morpho"
ETYMOLOGY="--enable etymology"
LIME="--enable lime"
TDB=""
DATE=""
VERBOSE=""
ENHANCE="false"

while getopts ":d:t:v:D:nmMeElLTVX" opt; do
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
      DATE="-D ${OPTARG}"
      ;;
    n)
      NETWORK="-n"
      ;;
    m)
      MORPHO="--enable morpho"
      ;;
    M)
      MORPHO=""
      ;;
    l)
      LIME="--enable lime"
      ;;
    L)
      LIME=""
      ;;
    e)
      ETYMOLOGY="--enable etymology"
      ;;
    E)
      ETYMOLOGY=""
      ;;
    T)
      TDB="--tdb"
      ;;
    X)
      ENHANCE="true"
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
shift $((OPTIND-1))

if [ $# -gt 0 ]
    then
    LANGS=$*
fi


TLANGS="fra,eng,por,deu,ell,rus,ita,fin,tur,jpn"
JAVA=java

#-Dorg.slf4j.simpleLogger.log.info.bliki.extensions.scribunto.engine.lua=trace
#-Dorg.slf4j.simpleLogger.log.org.getalp.dbnary=trace
#-Dorg.slf4j.simpleLogger.log.org.getalp.dbnary.OntolexBasedRDFDataHandler


if [ ! -z $VERBOSE ]
then
echo $JAVA -Xmx16g -Djava.net.useSystemProxies=true ${DEBUG} \
-cp ${HOME}/.m2/repository/org/getalp/dbnary-extractor/$VERSION/dbnary-extractor-$VERSION-jar-with-dependencies.jar \
    org.getalp.dbnary.cli.UpdateAndExtractDumps $VERBOSE $DATE $NETWORK $MORPHO $ETYMOLOGY $LIME $TDB -d $DIR -z  -k 1 $LANGS
fi

$JAVA -Xmx16g -Djava.net.useSystemProxies=true ${DEBUG} \
-cp ${HOME}/.m2/repository/org/getalp/dbnary-extractor/$VERSION/dbnary-extractor-$VERSION-jar-with-dependencies.jar \
    org.getalp.dbnary.cli.UpdateAndExtractDumps $VERBOSE $DATE $NETWORK $MORPHO $ETYMOLOGY $LIME $TDB -d $DIR -z  -k 1 $LANGS

ENHANCER=dbnary-enhancer
#-s http://dumps.wikimedia.org/
if [ $ENHANCE == 'true' ]
then
  # Enhancing translation (source disambiguation)
  if [ ! -z $VERBOSE ]
then
  echo $JAVA  -cp $HOME/.m2/repository/org/getalp/${ENHANCER}/${VERSION}/${ENHANCER}-${VERSION}-jar-with-dependencies.jar org.getalp.dbnary.enhancer.EnhanceLatestExtracts -d ${DIR}/extracts -z
fi

  $JAVA  -cp $HOME/.m2/repository/org/getalp/${ENHANCER}/${VERSION}/${ENHANCER}-${VERSION}-jar-with-dependencies.jar org.getalp.dbnary.enhancer.EnhanceLatestExtracts -d ${DIR}/extracts -z
fi

