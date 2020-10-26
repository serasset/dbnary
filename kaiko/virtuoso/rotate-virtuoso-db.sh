#!/usr/bin/env bash

## Test if bash version 4 as we need associative arrays.
if [[ "${BASH_VERSINFO:-0}" -lt 4 ]]; then
  echo >&2 "Need bash 4 version. Exiting."
  exit 1
fi

## Parse command line options
OPTIND=1 # Reset in case getopts has been used previously in the shell.

# Initialize our own variables:
askpass=false
password=''
verbose=false
DBNARY_USER_CONFIG_DIR="$HOME/.dbnary/"
DBNARY_ONTOLEX=$HOME/develop/wiktionary/extracts/ontolex
VIRTUOSODBLOCATION=/var/lib/virtuoso-opensource-7/
TEMPORARYPREFIX=/var/tmp/

function show_help() {
  echo "USAGE: $0 [-hvP] [-p password] [-P] [-d dir] [-c config] [-e ontolexdir] [-t tmp]"
  echo "OPTIONS:"
  echo "      h: display this help message."
  echo "      c: use provided value as the configuration directory (default value = $DBNARY_USER_CONFIG_DIR)."
  echo "         This directory should contain config and virtuoso.ini.tmpl files."
  echo "      d: use provided value as the virtuoso database location (default value = $VIRTUOSODBLOCATION)."
  echo "      e: use provided value as the dbnary folder containing all extraction data (older and latest ones. default value = $DBNARY_ONTOLEX)."
  echo "      t: use provided value as the prefix for temp folders (default value = $TEMPORARYPREFIX)."
  echo "      p: use provided password as the db password (default: password will be provided by DBPASSWD)."
  echo "      P: asks for a db password interactively (default: true if password is not provided)."
  echo "      v: uses verbose output."
}

while getopts "h?p:Pvc:l:t:d:" opt; do
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
  esac
done
DBNARYLATEST=${DBNARY_ONTOLEX}/latest

shift $((OPTIND - 1))

[ "$1" = "--" ] && shift

## Default values that will be overriden by configuration file
PATH=/sbin:/bin:/usr/sbin:/usr/bin:/opt/virtuoso-opensource/bin
VIRTUOSODAEMON=virtuoso-t
SERVERPORT=1112
SSLSERVERPORT=2112
WEBSERVERPORT=8899
VIRTUOSO_PLUGINS_HOSTING=/usr/lib/virtuoso-opensource-7/hosting
VAD_INSTALL_DIR=/usr/share/virtuoso-opensource-7/vad/
VSP_INSTALL_DIR=/var/lib/virtuoso-opensource-7/vsp/
BOOTSTRAPSQLTMPL=$script_dir/bootstrap.sql.tmpl


script_dir=$(dirname $(realpath $0))
bootstrap_ini=virtuoso.ini.bootstrap.tmpl
prod_ini=virtuoso.ini.prod.tmpl
## Read values from configuration file
[[ -f $DBNARY_USER_CONFIG_DIR/config ]] && source $DBNARY_USER_CONFIG_DIR/config
[[ x$VIRTUOSOINITMPL == "x" ]] && VIRTUOSOINITMPL=$DBNARY_USER_CONFIG_DIR/$bootstrap_ini
[[ -f $VIRTUOSOINITMPL ]] || VIRTUOSOINITMPL=$script_dir/$bootstrap_ini
if [[ ! -f $VIRTUOSOINITMPL ]]; then
  echo >&2 "Could not find virtuoso.ini template file."
  exit 1
fi


if ! command -v $VIRTUOSODAEMON ; then
  echo >&2 "Could not find virtuoso-t bin"
  exit 1
fi

if [ ! -d $DBNARYLATEST ]; then
  echo >&2 "Latest turtle data not available. $DBNARYLATEST does not exist."
  exit 1
fi

if [[ ! -w $VIRTUOSODBLOCATION ]]; then
  >&2 echo "Virtuoso database location '$VIRTUOSODBLOCATION' is not writable."
  exit 1
fi

## READING DB PASSWORD
if [[ $askpass == "true" || ${password}x == 'x' ]]; then
  echo "Enter your bootstrap database password : "
  IFS= read -s -p Password: password
  echo
fi

## Prepare the dataset directory
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
cat "$BOOTSTRAPSQLTMPL"
sed "s|@@DBBOOTSTRAPFOLDER@@|$DBBOOTSTRAPFOLDER|g" <"$BOOTSTRAPSQLTMPL" |
  sed "s|@@DATASETDIR@@|$DATASETDIR|g" |
  sed "s|@@SERVERPORT@@|$SERVERPORT|g" |
  sed "s|@@SSLSERVERPORT@@|$SSLSERVERPORT|g" |
  sed "s|@@VAD_INSTALL_DIR@@|$VAD_INSTALL_DIR|g" |
  sed "s|@@VSP_INSTALL_DIR@@|$VSP_INSTALL_DIR|g" |
  sed "s|@@VIRTUOSO_PLUGINS_HOSTING@@|$VIRTUOSO_PLUGINS_HOSTING|g" |
  sed "s|@@WEBSERVERPORT@@|$WEBSERVERPORT|g" > $BOOTSTRAPSQL

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
  files=(${DATASETDIR}/*.ttl)
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
    bunzip2 ./*.ttl.bz2
  fi
)

## create the .graph files for all files in datasetdir
langRegex2='(..)_([^_]*)_(.*)'
langRegex3='(...)_([^_]*)_(.*)'
statsRegex2='(..)_([^_]*)_statistics(.*)'
for f in $DATASETDIR/*.ttl; do
  if [[ $f =~ $statsRegex2 ]]; then
    lg2=${BASH_REMATCH[1]}
    echo "http://kaiko.getalp.org/statistics/" >"$f.graph"
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
if [ ! -d "$DBBOOTSTRAPFOLDER" ]; then
  mkdir -p $DBBOOTSTRAPFOLDER
elif [ "$(ls -A $DBBOOTSTRAPFOLDER)" ]; then
  echo >&2 "Database Folder $DBBOOTSTRAPFOLDER exists but is not empty. Aborting."
  exit 1
fi

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
echoln --- Setting up indexing
RDF_OBJ_FT_RULE_ADD (null, null, 'All');
VT_INC_INDEX_DB_DBA_RDF_OBJ ();
echoln --- Populating lookup table
-- Run the following procedure using the Virtuoso isql program to populate label lookup tables periodically and activate the Label text box of the Entity Label Lookup tab:
urilbl_ac_init_db();
echoln --- Ranking IRIs
-- Run the following procedure using the Virtuoso isql program to calculate the IRI ranks. Note this should be run periodically as the data grows to re-rank the IRIs.
s_rank();
echoln "=== Indexing done                                    ===" ;

END

## Expand data by linking lexical entries when there is no homonymy
## TODO: do it at extraction post processing using JENA on the TDB, so that it may be possible
#        to also link in ambiguous cases ?
isql $SERVERPORT dba "$password" <<END
-- turn off transaction isolation to avoid reaching limits in transaction log
log_enable(2);
echoln "========================================================" ;
echoln "=== Linking translatableAs Lexical Entries           ===" ;
echoln "========================================================" ;
SPARQL INSERT
    { GRAPH <http://kaiko.getalp.org/dbnary/vartrans> {?sle vartrans:translatableAs ?tle} }
WHERE {
    { SELECT (sample(?sle) as ?sle), (sample(?le) as ?tle) WHERE {
      ?trans
        a dbnary:Translation ;
        dbnary:isTranslationOf ?sle ;
        dbnary:targetLanguage ?lg ;
        dbnary:writtenForm ?wf.
      ?sle a ontolex:LexicalEntry;
        lexinfo:partOfSpeech ?pos.
      ?le a ontolex:LexicalEntry;
        dct:language ?lg;
        ontolex:canonicalForm / ontolex:writtenRep ?wf;
        lexinfo:partOfSpeech ?pos.
      } GROUP BY ?trans
        HAVING (COUNT(*) = 1)
    }
};
checkpoint;
commit WORK;
checkpoint;
echoln "=== Loading done                                     ===" ;
END

#Shutdown the bootstrap database
isql $SERVERPORT dba "$password" <<END
checkpoint;
shutdown();
END

## COPY THE DATABASE NEAR THE VIRTUOSO DB
CURRENTDATETIMESTAMP=$(date +"%Y-%m-%d-%H-%M-%S")
NEWDBFOLDER=${VIRTUOSODBLOCATION}/db.$CURRENTDATETIMESTAMP
echo "Moving database to $NEWDBFOLDER"
mv $DBBOOTSTRAPFOLDER $NEWDBFOLDER

# TODO: Generate a production ready virtuoso.ini file
# TODO: stop virtuoso daemon and rotate db folders then restart virtuoso daemon
