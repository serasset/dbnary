#!/bin/bash

VERSION=3.0.0b3-SNAPSHOT
#LANGS="fr en de pt it fi ru el tr ja es bg pl"
DIR=${DBNARY_DIR:-$HOME/develop/wiktionary}
LATEST=${DIR}/extracts/ontolex/latest
VERBOSE=""
HELP=""

while getopts "v:d:Vh" opt; do
  case $opt in
    d)
      LATEST=$OPTARG
      ;;
    v)
      VERSION="${OPTARG}"
      ;;
    V)
      VERBOSE="-V"
      ;;
    h)
      HELP="-h"
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

  java -Xmx16G -cp "${HOME}/.m2/repository/org/getalp/dbnary-commands/$VERSION/dbnary-commands-$VERSION-uber-jar.jar" \
    org.getalp.dbnary.cli.CreateHDTVersions $VERBOSE $HELP $LATEST $@
