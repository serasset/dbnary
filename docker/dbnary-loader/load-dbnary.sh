#!/usr/bin/env bash

## Test if bash version 4 as we need associative arrays.
if [[ "${BASH_VERSINFO:-0}" -lt 4 ]]; then
  echo >&2 "Need bash 4 version. Exiting."
  exit 1
fi

echo "====== LOADING DBNARY DATA TO FUSEKI_BASE ========="

# Get environment variables with defaults
LANGUAGES=${LANGUAGES:-"no it fr en de pt fi ru el tr ja es bg pl nl sh sv lt mg id la ku"}
FEATURES=${FEATURES:-"statistics ontolex morphology lime etymology enhancement exolex_ontolex"}
MIRROR=${MIRROR:-"http://kaiko.getalp.org/static/ontolex"}
VERSION=${VERSION:-""}
KEEP_IN_CACHE=${KEEP_IN_CACHE:-"no"}

## Parse command line options
OPTIND=1 # Reset in case getopts has been used previously in the shell.

function show_help() {
  echo "USAGE: $0 [-h?c] [-l languages] [-m url] [-d version] [-f features]"
  echo "OPTIONS:"
  echo "      h: display this help message."
  echo "      l: use provided value as the languages to be loaded."
  echo "      f: use provided value as the features to be loaded."
  echo "      d: use provided value as the DBnary version (default is latest version)."
  echo "      m: use provided value as the dbnary mirror url from which data is loaded."
  echo "      c: keep downloaded compressed files in cache dir (use it when the cache dir is
bound with rw access to an host folder)."
}

while getopts "h?cl:m:d:f:" opt; do
  case "$opt" in
  h | \?)
    show_help
    exit 0
    ;;
  l)
    LANGUAGES=$OPTARG
    ;;
  f)
    FEATURES=$OPTARG
    ;;
  m)
    MIRROR=$OPTARG
    ;;
  d)
    VERSION=$OPTARG
    ;;
  c)
    KEEP_IN_CACHE=yes
    ;;
  esac
done

shift $((OPTIND - 1))

[ "$1" = "--" ] && shift

echo DBNARY_CACHE = ${DBNARY_CACHE}
echo MIRROR = ${MIRROR}
echo LANGUAGES = ${LANGUAGES}
echo FEATURES = ${FEATURES}
echo KEEP_IN_CACHE = ${KEEP_IN_CACHE}

if [ x$DBNARY_CACHE == x ]; then
  echo >&2 "The DBNARY_CACHE environment variable is unknown. Exiting."
  exit 1
fi
if [ x$DBNARY_TDB == x ]; then
  echo >&2 "The DBNARY_TDB environment variable is unknown. Exiting."
  exit 1
fi

[ -d "$DBNARY_CACHE" ] || mkdir -p $DBNARY_CACHE
[ -d "$DBNARY_TDB" ] || mkdir -p $DBNARY_TDB

# Copy configuration templates to FUSEKI_BASE
cp -r ${DBNARY_TEMPLATES}/* ${FUSEKI_BASE}

# Create a temporary dir and schedule it for cleanup if anything goes wrong
DBNARY_TMP=$(mktemp -d -t dbnary-XXXXXXXXXX)
echo >&2 Downloading extracts to ${DBNARY_TMP}
[ -d "${DBNARY_TMP}" ] || mkdir -p ${DBNARY_TMP}

# deletes the temp directory
function cleanup() {
  rm -rf "${DBNARY_TMP}"
  echo "Deleted temp working directory ${DBNARY_TMP}"
}
# register the cleanup function to be called on the EXIT signal
trap cleanup EXIT


# Prepare data loading
## Converting language codes
declare -A iso3Lang
iso3Lang[bg]=bul
iso3Lang[de]=deu
iso3Lang[el]=ell
iso3Lang[en]=eng
iso3Lang[es]=spa
iso3Lang[fi]=fin
iso3Lang[fr]=fra
iso3Lang[it]=ita
iso3Lang[ja]=jpn
iso3Lang[pl]=pol
iso3Lang[pt]=por
iso3Lang[ru]=rus
iso3Lang[tr]=tur
iso3Lang[nl]=nld
iso3Lang[sh]=shr
iso3Lang[sv]=swe
iso3Lang[lt]=lit
iso3Lang[id]=ind
iso3Lang[la]=lat
iso3Lang[mg]=mlg
iso3Lang[no]=nor
iso3Lang[bm]=bam
iso3Lang[ku]=kur

function getGraph() {
  local lg=$1
  local ft=$2

  local lg3=${iso3Lang[$lg]}
  if [[ "$ft" == "statistics" ]]; then
    echo "http://kaiko.getalp.org/statistics/"
  elif [[ "$ft" =~ ^exolex ]]; then
    echo "http://kaiko.getalp.org/dbnary/$lg3_exolex"
  else
    echo "http://kaiko.getalp.org/dbnary/$lg3"
  fi
}

CURL_FETCH_OPTS="-s -S --fail --location --max-redirs 3"
download() { # URL
    local folder="$1"
    local filename="$2"
    local URL=${MIRROR}/${folder}/${filename}

    if [ ! -e "${DBNARY_CACHE}/${folder}/${filename}" ]
    then
	    echo >&2 "Fetching $URL"
	    mkdir -p ${DBNARY_TMP}/${folder}
	    curl $CURL_FETCH_OPTS "$URL" --output "${DBNARY_TMP}/${folder}/${filename}" \
	        || { echo >&2 "Bad download of $FN" 2>&1 ; return 1 ; }
	    if [[ ${KEEP_IN_CACHE} == "yes" ]]; then
	      mkdir -p ${DBNARY_CACHE}/${folder}/
	      mv "${DBNARY_TMP}/${folder}/${filename}" "${DBNARY_CACHE}/${folder}/${filename}"
	      echo "${DBNARY_CACHE}/${folder}/${filename}"
	    else
	      echo "${DBNARY_TMP}/${folder}/${filename}"
	    fi
    else
	    echo >&2 "$FN already present"
	    echo "${DBNARY_CACHE}/${folder}/${filename}"
    fi
    return 0
}

function load() {
  lg=$1 ## language
  ft=$2 ## feature
  local suffix
  local folder

  if [ x${VERSION} != x ]; then
    suffix=_${VERSION}
    folder=${lg}
  else
    suffix=""
    folder=latest
  fi

  local filename=${lg}_dbnary_${ft}${suffix}.ttl.bz2
  echo `download $folder $filename`
}

for lg in ${LANGUAGES}; do
  for ft in ${FEATURES}; do
    file=`load $lg $ft`
    graph=`getGraph $lg $ft`
    echo >&2 "Loading $file in graph $graph"
    ## Do not specify a grph until we migrate to JENA 4.5.0+
    # bzcat $file | ${FUSEKI_HOME}/tdb2.tdbloader --loc ${DBNARY_TDB} --graph ${graph} --syntax=Turtle --
    bzcat $file | ${FUSEKI_HOME}/tdb2.tdbloader --loc ${DBNARY_TDB} --syntax=Turtle --
    if [[ $file =~ ^${DBNARY_TMP} ]]; then
      rm -f $file
    fi
  done
done

echo "====== END LOADING DATA ========="