#!/usr/bin/env bash

## Test if bash version 4 as we need associative arrays.
if [[ "${BASH_VERSINFO:-0}" -lt 4 ]]; then
  echo >&2 "Need bash 4 version. Exiting."
  exit 1
fi

## Parse command line options
OPTIND=1 # Reset in case getopts has been used previously in the shell.

# Initialize our own variables:
verbose=false
PREFIXDIR=$HOME/develop/wiktionary/extracts/
WIDOCOJAR=$HOME/lib/widoco-1.4.14-jar-with-dependencies.jar
JAVA=java
ONTOLOGY=./../../dbnary-ontology/src/main/resources/org/getalp/dbnary/dbnary.ttl

function show_help() {
  echo "USAGE: $0 [-hvP] [-p password] [-P] [-d dir] [-c config] [-l latestdir] [-t tmp]"
  echo "OPTIONS:"
  echo "      h: display this help message."
  echo "      d: use provided value as the directory containing datamodel description and extracts (default value = $PREFIXDIR)."
  echo "      v: uses verbose output."
}

function checkEnvironment() {
  command -v $JAVA >/dev/null 2>&1 || { echo >&2 "Java is not available.  Aborting."; exit 1; }
  [[ -r "$WIDOCOJAR" ]] || { echo >&2 "The widoco jar is not available at $WIDOCOJAR.  Aborting."; exit 1; }
  [[ -d "$PREFIXDIR" ]] || { echo >&2 "The directory $PREFIXDIR does not exist.  Aborting."; exit 1; }
}

function getOntologyVersion() {
echo Reading version from $1
  ONTOLOGY_VERSION=$(sed -ne 's/^.*owl:versionIRI.*\<http:\/\/kaiko\.getalp\.org\/dbnary\/\(.*\)\>.*$/\1/p' < $1)
}

while getopts "h?vd:" opt; do
  case "$opt" in
  h | \?)
    show_help
    exit 0
    ;;
  v)
    verbose=true
    ;;
  d)
    PREFIXDIR=$OPTARG
    ;;
  esac
done

shift $((OPTIND - 1))

[ "$1" = "--" ] && shift

checkEnvironment
getOntologyVersion $ONTOLOGY

OUTPUTDIR=$PREFIXDIR/datamodel/$ONTOLOGY_VERSION

echo "ONTOLOGY_VERSION = $ONTOLOGY_VERSION"
echo "OUTPUTDIR = $OUTPUTDIR"
echo -n "continue [y/N] : "
IFS= read continue
[[ "$continue" == "y" ]] || exit 0;

if [[ -d $OUTPUTDIR ]]
then
  echo $OUTPUTDIR already exists. Delete it and continue [y/N]
  IFS= read continue
  [[ "$continue" == "y" ]] || exit 0;
  rm -rf $OUTPUTDIR
fi

mkdir -p $OUTPUTDIR

$JAVA -jar $WIDOCOJAR -ontFile $ONTOLOGY \
  -outFolder $OUTPUTDIR -getOntologyMetadata -oops -rewriteAll -htaccess -uniteSections \
  -rewriteBase /static/datamodel/

pushd $PREFIXDIR/datamodel
[[ -L current ]] && rm current
ln -s $ONTOLOGY_VERSION current
popd

echo Should I deploy the generated files to the public web server [y/N]
IFS= read continue
[[ "$continue" == "y" ]] || exit 0;
rsync -avz $PREFIXDIR/datamodel lig-getalp.imag.fr:/opt/dbnary/static/datamodel
