#!/bin/bash

VERSION=3.0.0b3
#LANGS="fr en de pt it fi ru el tr ja es bg pl"
DIFFS=diffs
VERBOSE=""
SLACK=""

while getopts "v:d:Vs" opt; do
  case $opt in
    d)
      DIFFS=$OPTARG
      ;;
    v)
      VERSION="${OPTARG}"
      ;;
    V)
      VERBOSE="-v"
      ;;
    s)
      SLACK="--slack"
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

  set -v
  >&2 echo "Summarizing diffs in $DIFFS"
  java -Xmx16G -cp "${HOME}/.m2/repository/org/getalp/dbnary-commands/$VERSION/dbnary-commands-$VERSION-uber-jar.jar" \
    org.getalp.dbnary.cli.SummarizeDifferences $VERBOSE $SLACK $DIFFS
