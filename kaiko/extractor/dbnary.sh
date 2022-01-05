#!/bin/sh

DIR=${DBNARY_DIR:-.}
VERSION=2.4.3-SNAPSHOT
#LANGS="fr en de pt it fi ru el tr ja es bg pl"
LANGS="fr"

DEBUG=""
NETWORK=""
MORPHO="--enable morphology"
ETYMOLOGY="--enable etymology"
LIME="--enable lime"
ENHANCE="--enable enhancement"
STATS="--enable statistics"
TDB=""
DATE=""
VERBOSE=""
FORCE=""
CUT=""
COMPRESS="-z"

help() {
  echo "USAGE: $0 [OPTIONS] lg1 lg2..."
  echo "Update and extract wiktionary dumps with DBnary."
  echo "  By default, DBnary will fetch the latest available version on wikimedia dump mirror if it "
  echo "  is not already available on the local host."
  echo "  By default, if the extracted data already exists, it is not re-extracted."
  echo "where:"
  echo "  lg1 lg2... are language codes (usually 2 letter codes as used in wiktionary language editions)"
  echo "  with OPTIONS in:"
  echo "    -h                : display this help message"
  echo "    -T                : use TDB to store extracted data (necessary for big wiktionaries)"
  echo "    -V                : enable verbose mode"
  echo "    -f                : force download (if network is enabled) and extraction even if data is already available"
  echo "    -c  <n>           : cut extraction after n lexical entries."
  echo "    -d  <package>     : enable DEBUG log level for the specified java package"
  echo "    -t  <package>     : enable TRACE log level for the specified java package"
  echo "    -v  <version>     : use the specified version of DBnary extractor"
  echo "    -D  <version>     : use the specified version of wiktionary dumps (older dumps may not be available online)"
  echo "    -n                : do not fetch anything from the network but extract the latest dumps available on the local host"
  echo "    -m                : enable extraction of morphological data (enabled by default)"
  echo "    -M                : disable extraction of morphological data"
  echo "    -l                : enable extraction of LIME metadata (enabled by default)"
  echo "    -L                : disable extraction of LIME metadata"
  echo "    -s                : enable extraction of statistics (enabled by default)"
  echo "    -S                : disable extraction of statistics"
  echo "    -e                : enable extraction of etymology (enabled by default)"
  echo "    -E                : disable extraction of etymology"
  echo "    -x                : enable computation of data enhancement (e.g. translation source disambiguation, enabled by default)"
  echo "    -X                : disable computation of data enhancement"
  echo "    -z                : compress output using bz2 (enabled by default)"
  echo "    -Z                : do not compress output"
}

while getopts ":d:t:v:D:c:nmMeElLsSTVxXhfzZ" opt; do
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
    c)
      CUT="--sample ${OPTARG}"
      ;;
    n)
      NETWORK="-n"
      ;;
    m)
      MORPHO="--enable morphology"
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
    s)
      STATS="--enable statistics"
      ;;
    S)
      STATS=""
      ;;
    e)
      ETYMOLOGY="--enable etymology"
      ;;
    E)
      ETYMOLOGY=""
      ;;
    x)
      ENHANCE="--enable enhancer"
      ;;
    X)
      ENHANCE=""
      ;;
    z)
      COMPRESS="-z"
      ;;
    Z)
      COMPRESS=""
      ;;
    T)
      TDB="--tdb"
      ;;
    V)
      VERBOSE="-v"
      ;;
    f)
      FORCE="-f"
      ;;
    h)
      help
      exit 0
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

JAVA=java

#-Dorg.slf4j.simpleLogger.log.info.bliki.extensions.scribunto.engine.lua=trace
#-Dorg.slf4j.simpleLogger.log.org.getalp.dbnary=trace
#-Dorg.slf4j.simpleLogger.log.org.getalp.dbnary.OntolexBasedRDFDataHandler


if [ ! -z $VERBOSE ]
then
echo $JAVA -Xmx8g -Djava.net.useSystemProxies=true ${DEBUG} \
-cp ${HOME}/.m2/repository/org/getalp/dbnary-commands/$VERSION/dbnary-commands-$VERSION-uber-jar.jar \
    org.getalp.dbnary.cli.UpdateAndExtractDumps $VERBOSE $FORCE $CUT $DATE $NETWORK $MORPHO $ETYMOLOGY $LIME $ENHANCE $STATS $TDB -d $DIR $COMPRESS  -k 1 $LANGS
fi

$JAVA -Xmx8g -Djava.net.useSystemProxies=true ${DEBUG} \
-cp ${HOME}/.m2/repository/org/getalp/dbnary-commands/$VERSION/dbnary-commands-$VERSION-uber-jar.jar \
    org.getalp.dbnary.cli.UpdateAndExtractDumps $VERBOSE $FORCE $CUT $DATE $NETWORK $MORPHO $ETYMOLOGY $LIME $ENHANCE $STATS $TDB -d $DIR $COMPRESS  -k 1 $LANGS

