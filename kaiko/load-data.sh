#!/bin/bash

## Test if bash version 4 as we need associative arrays.
if [[ $BASH_VERSION != 4.* ]]
then
    echo "Need bash 4 version. Exiting."
    exit -1
fi

## Bootstrapping a virtuoso db.

PREFIX=$HOME/develop
if [[ ! $# -eq 0 ]]
then
    PREFIX=$1
fi

source config.sh

DBNARYLATEST=$HOME/dev/wiktionary/extracts/ontolex/latest

test -x $DAEMON || (echo "Could not find virtuoso-t bin" && exit 0)

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


if [ ! -d "$EMPTYDBFOLDER" ] ; then
    echo "No Bootstrap DB folder, cannot load data."
    exit -1
elif [[ ! -f $EMPTYDBFOLDER/virtuoso.db ]]; then
    echo "Bootstrap database file does not exists, cannot load data."
    exit -1
fi

if [ ! -d "$DBFOLDER" ] ; then
    cp -r $EMPTYDBFOLDER $DBFOLDER
    sed "s|@@DBFOLDER@@|$DBFOLDER|g" < $VIRTUOSOINITMPL | \
    sed "s|@@DATASETDIR@@|$DATASETDIR|g" | \
    sed "s|@@SERVERPORT@@|$SERVERPORT|g" | \
    sed "s|@@SSLSERVERPORT@@|$SSLSERVERPORT|g" | \
    sed "s|@@WEBSERVERPORT@@|$WEBSERVERPORT|g" > "$DBFOLDER"/virtuoso.ini
elif [[ ! -f $EMPTYDBFOLDER/virtuoso.db ]]; then
    exit -1
else
    echo "Already existing database folder in bootstrap folder, should I load the data in the existing DB? (y/N):"
    read answer
    if [ z$answer != zy ]; then
        echo "delete existing bootstrap DB and restart the loading script."
        exit -1
    fi
fi


if [ ! -d $DBNARYLATEST ]
then
    echo "Latest turtle data not available."
    exit -1
fi

if [ ! -d "$DATASETDIR" ]
then
    mkdir -p "$DATASETDIR"
fi


## Prepare the dataset directory
(
  shopt -s nullglob
  files=($DATASETDIR/*.ttl)
  if [[ "${#files[@]}" -gt 0 ]] ; then
    echo "Dataset already exists and is not empty, assuming its content is up to date."
  else
    echo "Copying and expanding latest extracts."
    ## Ontolex normal dumps
    cp $DBNARYLATEST/*.ttl.bz2 "$DATASETDIR"
    ## TODO: expand Disambiguated translations + foreign data ? + etymology
    pushd "$DATASETDIR"
    bunzip2 ./*.ttl.bz2
  fi
)



## create the .graph files for all files in datasetdir
## DONE: detect the graph (dbnary or dilaf ?)
langRegex2='(..)_([^_]*)_(.*)'
langRegex3='(...)_([^_]*)_(.*)'
for f in $DATASETDIR/*.ttl
do
    if [[ $f =~ $langRegex2 ]]
    then
        lg2=${BASH_REMATCH[1]}
        graph=${BASH_REMATCH[2]}
        lg3=${iso3Lang[$lg2]}
        echo "http://kaiko.getalp.org/$graph/$lg3" > "$f.graph"
    elif [[ $f =~ $langRegex3 ]]
    then
        lg3=${BASH_REMATCH[1]}
        graph=${BASH_REMATCH[2]}
        echo "http://kaiko.getalp.org/$graph/$lg3" > "$f.graph"
    fi
done

## Launch virtuoso to load the data into DB
echo "Launching daemon."
pushd "$DBFOLDER" || exit -1
$DAEMON -c $NAME +wait &
wait

## connect to isql and load all the data
echo "Enter your bootstrap database password : "
IFS= read -s  -p Password: pwd

isql $SERVERPORT dba "$pwd" <<END
ld_dir ('$DATASETDIR', '*.ttl', 'http://kaiko.getalp.org/dbnary');

-- do the following to see which files were registered to be added:
SELECT * FROM DB.DBA.LOAD_LIST;
-- if unsatisfied use:
-- delete from DB.DBA.LOAD_LIST;
rdf_loader_run();

-- do nothing too heavy while data is loading
checkpoint;
commit WORK;
checkpoint;
END

## (TODO: create the virtlabels for correct facetted browsing)

## (TODO: load the owl files that will be used for reasoning)
## ld_dir ('/opt/datasets/dbnary/', '*.owl','http://kaiko.getalp.org/dbnaryetymology');
## rdfs_rule_set('etymology_ontology','http://kaiko.getalp.org/dbnaryetymology');
## And then in queries I use
## define input:inference "etymology_ontology";

## index facetted browsing
isql $SERVERPORT dba "$pwd" <<END
sparql SELECT COUNT(*) WHERE { ?s ?p ?o } ;
sparql SELECT ?g COUNT(*) { GRAPH ?g {?s ?p ?o.} } GROUP BY ?g ORDER BY DESC 2;

-- Build Full Text Indexes by running the following commands using the Virtuoso isql program
-- With this rule added, all text in all graphs will be indexed...
RDF_OBJ_FT_RULE_ADD (null, null, 'All');
VT_INC_INDEX_DB_DBA_RDF_OBJ ();
-- Run the following procedure using the Virtuoso isql program to populate label lookup tables periodically and activate the Label text box of the Entity Label Lookup tab:
urilbl_ac_init_db();
-- Run the following procedure using the Virtuoso isql program to calculate the IRI ranks. Note this should be run periodically as the data grows to re-rank the IRIs.
s_rank();
shutdown();
END

