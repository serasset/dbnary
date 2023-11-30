#!/usr/bin/env bash

if [ ! -t 0 ]; then
  exec 1>> /var/log/dbnary/dbnary.log 2>&1
fi

## Test if bash version 4 as we need associative arrays.
if [[ "${BASH_VERSINFO:-0}" -lt 4 ]]; then
  echo >&2 "Need bash 4 version. Exiting."
  exit 1
fi

echo "====== Rotating VIRTUOSO DB ========="

## Parse command line options
OPTIND=1 # Reset in case getopts has been used previously in the shell.

askpass=false
password=''
verbose=false
FORCE=false
DBNARY_USER_CONFIG_DIR="$HOME/.dbnary/"
DBNARY_ONTOLEX=$HOME/develop/wiktionary/extracts/ontolex
VIRTUOSODBLOCATION=/var/lib/virtuoso-opensource-7/
TEMPORARYPREFIX=/var/tmp/

function show_help() {
  echo "USAGE: $0 [-hvPf] [-p password] [-P] [-d dir] [-c config] [-e ontolexdir] [-t tmp]"
  echo "OPTIONS:"
  echo "      h: display this help message."
  echo "      c: use provided value as the configuration directory (default value = $DBNARY_USER_CONFIG_DIR)."
  echo "         This directory should contain config and virtuoso.ini.tmpl files."
  echo "      d: use provided value as the virtuoso database location (default value = $VIRTUOSODBLOCATION)."
  echo "      e: use provided value as the dbnary folder containing all extraction data (older and latest ones. default value = $DBNARY_ONTOLEX)."
  echo "      t: use provided value as the prefix for temp folders (default value = $TEMPORARYPREFIX)."
  echo "      p: use provided password as the db password (default: password will be asked interactively)."
  echo "      P: asks for a db password interactively (default: true if password is not provided)."
  echo "      f: force the database preparation even if the current dump is already deployed."
  echo "      v: uses verbose output."
}

while getopts "h?fp:Pvc:e:t:d:" opt; do
  case "$opt" in
  h | \?)
    show_help
    exit 0
    ;;
  v)
    verbose=true
    ;;
  p)
    password=$OPTARG
    ;;
  P)
    askpass=true
    ;;
  c)
    DBNARY_USER_CONFIG_DIR=$OPTARG
    ;;
  e)
    DBNARY_ONTOLEX=$OPTARG
    ;;
  d)
    VIRTUOSODBLOCATION=$OPTARG
    ;;
  t)
    TEMPORARYPREFIX=$OPTARG
    ;;
  f)
    FORCE=true
  esac
done

shift $((OPTIND - 1))

[ "$1" = "--" ] && shift

## Default values that will be overridden by configuration file
PATH=/sbin:/bin:/usr/sbin:/usr/bin:/opt/virtuoso-opensource/bin
VIRTUOSODAEMON=virtuoso-t
SERVERPORT=1112
SSLSERVERPORT=2112
WEBSERVERPORT=8899
VIRTUOSO_PLUGINS_HOSTING=/usr/lib/virtuoso-opensource-7/hosting
VAD_INSTALL_DIR=/usr/share/virtuoso-opensource-7/vad/
VSP_INSTALL_DIR=/var/lib/virtuoso-opensource-7/vsp/

script_dir=$(dirname "$(realpath $0)")
BOOTSTRAPSQLTMPL=$script_dir/bootstrap.sql.tmpl
bootstrap_ini=virtuoso.ini.bootstrap.tmpl
prod_ini=virtuoso.ini.prod.tmpl
#prod_ini=virtuoso.ini.prod.tmpl
## Read values from configuration file
[[ -f $DBNARY_USER_CONFIG_DIR/config ]] && source $DBNARY_USER_CONFIG_DIR/config

DBNARYLATEST=${DBNARYLATEST:-${DBNARY_ONTOLEX}/latest}
DBFOLDER=${VIRTUOSODBLOCATION}/db

[[ x$VIRTUOSOINITMPL == "x" ]] && VIRTUOSOINITMPL="$DBNARY_USER_CONFIG_DIR/$bootstrap_ini"
[[ -f $VIRTUOSOINITMPL ]] || VIRTUOSOINITMPL="$script_dir/$bootstrap_ini"
if [[ ! -f $VIRTUOSOINITMPL ]]; then
  echo >&2 "Could not find bootstrap virtuoso.ini template file."
  exit 1
fi
[[ x$VIRTUOSOPRODINITMPL == "x" ]] && VIRTUOSOPRODINITMPL="$DBNARY_USER_CONFIG_DIR/$prod_ini"
[[ -f $VIRTUOSOPRODINITMPL ]] || VIRTUOSOPRODINITMPL="$script_dir/$prod_ini"
if [[ ! -f $VIRTUOSOPRODINITMPL ]]; then
  echo >&2 "Could not find production virtuoso.ini template file ${VIRTUOSOPRODINITMPL}."
  exit 1
fi


BOOTSTRAPSQL=$script_dir/bootstrap.sql

if ! command -v $VIRTUOSODAEMON ; then
  echo >&2 "Could not find virtuoso-t bin"
  exit 1
fi

if [ ! -d "$DBNARYLATEST" ]; then
  echo >&2 "Latest turtle data not available. $DBNARYLATEST does not exist."
  exit 1
fi

if [[ ! -w "$VIRTUOSODBLOCATION" ]]; then
  echo >&2 "Virtuoso database location '$VIRTUOSODBLOCATION' is not writable."
  exit 1
fi


## Utility functions
function virtuoso_db_versions() {
  for dump_folder in $1/db.*; do
    dump_version=$(basename "${dump_folder}")
    dump_version=${dump_version##db.}
    dump_version=${dump_version%%.*}
    echo "${dump_version}"
  done
}

function virtuoso_latest_version() {
  virtuoso_db_versions "$1" | sort -r | head -n 1
}

function latest_versions() {
  for ontolex_extract in $1/??_dbnary_ontolex.ttl.bz2; do
    target=$(readlink "$ontolex_extract")
    if [ x$target != x ]; then
      target=$(basename $target)
      target=${target%%.*}
      target=${target##*_}
      echo "${target}"
    else
      echo X
    fi
  done
}

function all_unique_extraction_version() {
  latest_versions "$1" | sort | uniq
}

# latest_full_extraction_version returns the date part of the dump (e.g. 20201201) if ALL
# latest extracted data links to this dump version. If some extracted data is out of sync with the
# other, the function returns an empty string.
function latest_full_extraction_version() {
  nvers=$(all_unique_extraction_version $1 | wc -l)
  if [ "$nvers" -eq 1 ]; then
    version=$(all_unique_extraction_version "$1")
  else
    version=""
  fi
  echo "$version"
}

extractversion=$(latest_full_extraction_version "${DBNARYLATEST}")
virtuosoversion=$(virtuoso_latest_version "${VIRTUOSODBLOCATION}")

echo >&2 "Latest full extraction version: ${extractversion}"
echo >&2 "Current virtuoso version: ${virtuosoversion}"

if [[ ${FORCE} == true ]] ; then
  echo >&2 "Forcibly creating new DB folder."
elif [ "x${extractversion}" == "x" ]; then
  echo >&2 "The extracted versions are incoherent. Aborting rotation."
  exit
elif [ "${extractversion}" == "${virtuosoversion}" ]; then
  echo >&2 "Current extracted version is already active. Aborting rotation."
  exit
fi

## READING DB PASSWORD
if [[ $askpass == "true" || ${password}x == 'x' ]]; then
  echo "Enter your bootstrap database password : "
  IFS= read -s -p Password: password
  echo
fi

# Prepare the dataset directory
## Converting language codes
declare -A iso3Lang
iso3Lang[bg]="bul"
iso3Lang[de]="deu"
iso3Lang[el]="ell"
iso3Lang[en]="eng"
iso3Lang[es]="spa"
iso3Lang[fi]="fin"
iso3Lang[fr]="fra"
iso3Lang[it]="ita"
iso3Lang[ja]="jpn"
iso3Lang[pl]="pol"
iso3Lang[pt]="por"
iso3Lang[ru]="rus"
iso3Lang[tr]="tur"
iso3Lang[nl]="nld"
iso3Lang[sh]="shr"
iso3Lang[sv]="swe"
iso3Lang[lt]="lit"
iso3Lang[id]="ind"
iso3Lang[la]="lat"
iso3Lang[mg]="mlg"
iso3Lang[no]="nor"
iso3Lang[bm]="bam"
iso3Lang[ku]="kur"
iso3Lang[zh]="zho"
iso3Lang[ca]="cat"
iso3Lang[ga]="gle"


SAVETMPDIR=$TMPDIR
export TMPDIR=$TEMPORARYPREFIX
DATASETDIR=$(mktemp -d 2>/dev/null || mktemp -d -t 'dbnary-dataset' || exit 1)
[[ "$verbose" == "true" ]] && echo >&2 "Creating dataset directory : $DATASETDIR"
if [ ! -d "$DATASETDIR" ]; then
  echo >&2 "Could not create temporary dir. Aborting."
  exit 1
fi
DBBOOTSTRAPFOLDER=$(mktemp -d 2>/dev/null || mktemp -d -t 'dbnary-db' || exit 1)
[[ "$verbose" == "true" ]] && echo >&2 "Creating bootstrap database directory : $DBBOOTSTRAPFOLDER"
if [ ! -d "$DBBOOTSTRAPFOLDER" ]; then
  echo >&2 "Could not create temporary dir. Aborting."
  exit 1
fi
TMPDIR=$SAVETMPDIR

# deletes the temp directory
function cleanup() {
  rm -rf "$DATASETDIR"
  echo "Deleted temp working directory $DATASETDIR"
  rm -rf "$DBBOOTSTRAPFOLDER"
  echo "Deleted temp working directory $DBBOOTSTRAPFOLDER"
}

# register the cleanup function to be called on the EXIT signal
trap cleanup EXIT

## Prepare bootstrap.sql file
BOOTSTRAPSQL="$DBBOOTSTRAPFOLDER"/bootstrap.sql
echo "Preparing $BOOTSTRAPSQLTMPL"
#cat "$BOOTSTRAPSQLTMPL"
sed "s|@@DBBOOTSTRAPFOLDER@@|$DBBOOTSTRAPFOLDER|g" <"$BOOTSTRAPSQLTMPL" |
  sed "s|@@DATASETDIR@@|$DATASETDIR|g" |
  sed "s|@@SERVERPORT@@|$SERVERPORT|g" |
  sed "s|@@SSLSERVERPORT@@|$SSLSERVERPORT|g" |
  sed "s|@@VAD_INSTALL_DIR@@|$VAD_INSTALL_DIR|g" |
  sed "s|@@VSP_INSTALL_DIR@@|$VSP_INSTALL_DIR|g" |
  sed "s|@@VIRTUOSO_PLUGINS_HOSTING@@|$VIRTUOSO_PLUGINS_HOSTING|g" |
  sed "s|@@WEBSERVERPORT@@|$WEBSERVERPORT|g" >$BOOTSTRAPSQL

if [[ $verbose == "true" ]]; then
  echo "Virtuoso Daemon: $VIRTUOSODAEMON"
  echo "Latest extracts: $DBNARYLATEST"
  echo "Virtuoso ini template: $VIRTUOSOINITMPL"
  echo "Temporary dataset folder: $DATASETDIR"
  echo "Temporary bootstrap folder: $DBBOOTSTRAPFOLDER"
  echo "bootstrap sql file: $BOOTSTRAPSQL"
fi

# TODO: use a fixed dataset dir for dataset outside of dbnary latest folder.
(
  shopt -s nullglob
  files=("${DATASETDIR}"/*.ttl)
  if [[ "${#files[@]}" -gt 0 ]]; then
    echo >&2 "Dataset already exists and is not empty, assuming its content is up to date."
  else
    echo >&2 "Copying and expanding latest extracts."
    ## Ontolex normal dumps
    cp ${DBNARYLATEST}/*.ttl.bz2 "$DATASETDIR"
    ## Add all (including older) statistics files so that the whole datacube is available in the DB.
    cp ${DBNARY_ONTOLEX}/??/??_dbnary_statistics_*.ttl.bz2 "$DATASETDIR"
    ## TODO: expand Disambiguated translations + foreign data ? + etymology
    pushd "$DATASETDIR"
    # Decompress in parallel
    #bunzip2 ./*.ttl.bz2
    find ./*.ttl.bz2 -print0 | xargs -0 -n 1 -P 6 bunzip2
  fi
)

## create the .graph files for all files in datasetdir
langRegex2='(..)_([^_]*)_(.*)'
langRegex3='(...)_([^_]*)_(.*)'
statsRegex2='(..)_(.*)_statistics(.*)'
exolexRegex2='(..)_([^_]*)_exolex_(.*)'
for f in $DATASETDIR/*.ttl; do
  if [[ $f =~ $statsRegex2 ]]; then
    lg2=${BASH_REMATCH[1]}
    echo "http://kaiko.getalp.org/statistics/" >"$f.graph"
  elif [[ $f =~ $exolexRegex2 ]]; then
    lg2=${BASH_REMATCH[1]}
    graph=${BASH_REMATCH[2]}
    lg3=${iso3Lang[$lg2]}
    echo "http://kaiko.getalp.org/$graph/${lg3}_exolex/" >"$f.graph"
  elif [[ $f =~ $langRegex2 ]]; then
    lg2=${BASH_REMATCH[1]}
    graph=${BASH_REMATCH[2]}
    lg3=${iso3Lang[$lg2]}
    echo "http://kaiko.getalp.org/$graph/$lg3" >"$f.graph"
  elif [[ $f =~ $langRegex3 ]]; then
    lg3=${BASH_REMATCH[1]}
    graph=${BASH_REMATCH[2]}
    echo "http://kaiko.getalp.org/$graph/$lg3" >"$f.graph"
  fi
done

# TODO: the folder is now a new temporary folder
#if [ ! -d "$DBBOOTSTRAPFOLDER" ]; then
#  mkdir -p $DBBOOTSTRAPFOLDER
#elif [ "$(ls -A $DBBOOTSTRAPFOLDER)" ]; then
#  echo >&2 "Database Folder $DBBOOTSTRAPFOLDER exists but is not empty. Aborting."
#  exit 1
#fi

sed "s|@@DBBOOTSTRAPFOLDER@@|$DBBOOTSTRAPFOLDER|g" <"$VIRTUOSOINITMPL" |
  sed "s|@@DATASETDIR@@|$DATASETDIR|g" |
  sed "s|@@SERVERPORT@@|$SERVERPORT|g" |
  sed "s|@@SSLSERVERPORT@@|$SSLSERVERPORT|g" |
  sed "s|@@VAD_INSTALL_DIR@@|$VAD_INSTALL_DIR|g" |
  sed "s|@@VSP_INSTALL_DIR@@|$VSP_INSTALL_DIR|g" |
  sed "s|@@VIRTUOSO_PLUGINS_HOSTING@@|$VIRTUOSO_PLUGINS_HOSTING|g" |
  sed "s|@@WEBSERVERPORT@@|$WEBSERVERPORT|g" >"$DBBOOTSTRAPFOLDER"/virtuoso.ini

## CREATING A NEW EMPTY DATABASE WITH NECESSARY SETTINGS

## Launch virtuoso to create the new DB
echo "Launching daemon."
pushd "$DBBOOTSTRAPFOLDER" || exit 1
$VIRTUOSODAEMON -c virtuoso +wait &
daemon_pid=$!
echo launched deamon
wait
echo deamon launched and initialized

## connect to isql to load the different configurations
echo isql $SERVERPORT dba dba $BOOTSTRAPSQL
isql $SERVERPORT dba dba $BOOTSTRAPSQL

echo bootstrap done, changing password.
## Now change admin passwords and shutdown the database.
isql $SERVERPORT dba dba <<END
user_change_password('dba','dba', '$password');
user_change_password('dav','dav', '$password');
exit();
END

echo "Reconnecting to the DB to load all data."
isql $SERVERPORT dba "$password" <<END
ld_dir ('$DATASETDIR', '*.ttl', 'http://kaiko.getalp.org/dbnary');

-- do the following to see which files were registered to be added:
SELECT * FROM DB.DBA.LOAD_LIST;
-- if unsatisfied use:
-- delete from DB.DBA.LOAD_LIST;
echoln "========================================================" ;
echoln "=== Loading previously shown graphs                  ===" ;
echoln "========================================================" ;
rdf_loader_run();

-- do nothing too heavy while data is loading
checkpoint;
commit WORK;
checkpoint;

echoln "========================================================" ;
echoln "=== Looking for errors while loading graphs          ===" ;
echoln "========================================================" ;
-- Check the set of loaded files to see if errors appeared during load.
select * from DB.DBA.LOAD_LIST where ll_error IS NOT NULL;

echoln "=== Loading done                                     ===" ;
END

## (TODO: create the virtlabels for correct facetted browsing)
## not really useful as the default configuration indexes all string values

## (TODO: load the owl files that will be used for reasoning)
## ld_dir ('/opt/datasets/dbnary/', '*.owl','http://kaiko.getalp.org/dbnaryetymology');
## rdfs_rule_set('etymology_ontology','http://kaiko.getalp.org/dbnaryetymology');
## And then in queries I use
## define input:inference "etymology_ontology";

## (TODO: Load all statistics from the current extracts AND from all previous ones).

## index strings for faceted browsing
isql $SERVERPORT dba "$password" <<END
echoln "========================================================" ;
echoln "=== Stats on loaded graphs                           ===" ;
echoln "========================================================" ;

sparql SELECT COUNT(*) WHERE { ?s ?p ?o } ;
sparql SELECT ?g COUNT(*) { GRAPH ?g {?s ?p ?o.} } GROUP BY ?g ORDER BY DESC 2;

echoln "========================================================" ;
echoln "=== Beginning full text indexing on loaded graphs    ===" ;
echoln "========================================================" ;

-- Build Full Text Indexes by running the following commands using the Virtuoso isql program
-- With this rule added, all text in all graphs will be indexed...
echoln "--- Setting up indexing" ;
RDF_OBJ_FT_RULE_ADD (null, null, 'All');
VT_INC_INDEX_DB_DBA_RDF_OBJ ();
echoln "--- Populating lookup table" ;
-- Run the following procedure using the Virtuoso isql program to populate label lookup tables periodically and activate the Label text box of the Entity Label Lookup tab:
urilbl_ac_init_db();
echoln "--- Ranking IRIs" ;
-- Run the following procedure using the Virtuoso isql program to calculate the IRI ranks. Note this should be run periodically as the data grows to re-rank the IRIs.
s_rank();
checkpoint;
commit WORK;
checkpoint;
echoln "=== Indexing done                                    ===" ;

END

## Expand data by linking lexical entries when there is no homonymy
## TODO: do it at extraction post processing using JENA on the TDB, so that it may be possible
#        to also link in ambiguous cases ?
#isql $SERVERPORT dba "$password" <<END
#-- turn off transaction isolation to avoid reaching limits in transaction log
#log_enable(2);
#echoln "========================================================" ;
#echoln "=== Linking translatableAs Lexical Entries           ===" ;
#echoln "========================================================" ;
#SPARQL INSERT
#    { GRAPH <http://kaiko.getalp.org/dbnary/vartrans> {?sle vartrans:translatableAs ?tle} }
#WHERE {
#    { SELECT (sample(?sle) as ?sle), (sample(?le) as ?tle) WHERE {
#      ?trans
#        a dbnary:Translation ;
#        dbnary:isTranslationOf ?sle ;
#        dbnary:targetLanguage ?lg ;
#        dbnary:writtenForm ?wf.
#      ?sle a ontolex:LexicalEntry;
#        lexinfo:partOfSpeech ?pos.
#      ?le a ontolex:LexicalEntry;
#        dcterms:language ?lg;
#        rdfs:label ?wf;
#        lexinfo:partOfSpeech ?pos.
#      FILTER (REGEX(STR(?le), "^http://kaiko.getalp.org/dbnary/.../[^_]")) .
#      } GROUP BY ?trans
#        HAVING (COUNT(*) = 1)
#    }
#};
#checkpoint;
#commit WORK;
#checkpoint;
#echoln "=== Loading done                                     ===" ;
#END

#Shutdown the bootstrap database
isql $SERVERPORT dba "$password" <<END
checkpoint;
shutdown();
END

## PREPARE THE DB FOLDER FOR PROD.
#VIRTUOSOINIPROD=$script_dir/virtuoso.prod.ini

sed "s|@@DBFOLDER@@|$DBFOLDER|g" <"$VIRTUOSOPRODINITMPL" |
  sed "s|@@DATASETDIR@@|$DATASETDIR|g" |
  sed "s|@@SERVERPORT@@|$SERVERPORT|g" |
  sed "s|@@SSLSERVERPORT@@|$SSLSERVERPORT|g" |
  sed "s|@@VAD_INSTALL_DIR@@|$VAD_INSTALL_DIR|g" |
  sed "s|@@VSP_INSTALL_DIR@@|$VSP_INSTALL_DIR|g" |
  sed "s|@@VIRTUOSO_PLUGINS_HOSTING@@|$VIRTUOSO_PLUGINS_HOSTING|g" |
  sed "s|@@WEBSERVERPORT@@|$WEBSERVERPORT|g" >"$DBBOOTSTRAPFOLDER"/virtuoso.ini

#cp ${VIRTUOSOINIPROD} ${DBBOOTSTRAPFOLDER}/virtuoso.ini

## COPY THE DATABASE NEAR THE VIRTUOSO DB
CURRENTDATETIMESTAMP=$(date +"%Y-%m-%d-%H-%M-%S")
NEWDBFOLDER=${VIRTUOSODBLOCATION}/db.${CURRENTDATETIMESTAMP}
echo "Moving database to $NEWDBFOLDER"
mv $DBBOOTSTRAPFOLDER $NEWDBFOLDER
# renaming the new DB folder so that it will be deployed
echo "Renaming database folder $NEWDBFOLDER -->  ${VIRTUOSODBLOCATION}/db.${extractversion}.next"
mv $NEWDBFOLDER ${VIRTUOSODBLOCATION}/db.${extractversion}.next

echo "====== /END/ Rotating VIRTUOSO DB ========="
