#!/bin/bash

RDFDIFF=rdfdiff

LANGS="fr"
MODEL="ontolex"
PREVIOUS_VERSION=latest.before
NEXT_VERSION=latest.now
DIFFS=diffs
VERBOSE=""
FORCETDB=""

while getopts ":m:f:t:d:x:vT" opt; do
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
      VERBOSE="-v"
      ;;
    x)
      RDFDIFF=$OPTARG
      ;;
    T)
      FORCETDB="true"
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

if ! command -v $RDFDIFF ; then
  env
  echo >&2 "Could not find rdfdiff command, aborting..."
  exit 1
fi

prepareTDBifTooBig() {
  local ttlfile=$1
  if [ -f "$1" ]
  then
    fsize="$(wc -c <"$ttlfile")"
    if [[ "$fsize" -gt 1000000000 || "x$FORCETDB" == "xtrue" ]]
    then
        #More than 1G -> TDB
        >&2 echo "Preparing TDB2 from big ttl :  ${ttlfile}"
        tdbfile=${ttlfile}.tdb
        if [ -d "$tdbfile" ]
        then
            >&2 echo "TDB2 directory alrady exists, assuming it's OK:  $tdbfile"
            >&2 echo "If not, remove directory and relaunch command."
        else
            >&2 tdb2.tdbloader --loc "$tdbfile" "$ttlfile"
        fi
        echo "$tdbfile"
        return
    fi
  fi
  echo "$ttlfile"
}

for l in $LANGS
do
  set -v
    before="$(prepareTDBifTooBig $PREVIOUS_VERSION/${l}_dbnary_${MODEL}*.ttl)"
    now="$(prepareTDBifTooBig $NEXT_VERSION/${l}_dbnary_${MODEL}*.ttl)"
    >&2 echo "Comparing ${before} and ${now}"
  $RDFDIFF $VERBOSE "${before}" "${now}" > "$DIFFS/${l}_lost_${MODEL}.ttl" ;
  $RDFDIFF $VERBOSE "${now}" "${before}" > "$DIFFS/${l}_gain_${MODEL}.ttl" ;
done
