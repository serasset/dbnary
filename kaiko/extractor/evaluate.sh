#!/bin/bash
VALIDATION_LANGUAGES=$@

if [[ ! -d dbnary ]]
then
  ./prepare_evaluation.sh
fi

cd dbnary
echo "Requested languages: " $VALIDATION_LANGUAGES
source ./kaiko/cicd/settings
echo "Languages: " $LANGS

WIKTIONARY_DIR=target/wiktionary/
mkdir -p $WIKTIONARY_DIR/$NEXT_VERSION
mkdir -p $WIKTIONARY_DIR/$PREVIOUS_VERSION
ln -s $HOME/dev/wiktionary/tmp/dumps $WIKTIONARY_DIR/$NEXT_VERSION/dumps
ln -s $HOME/dev/wiktionary/tmp/dumps $WIKTIONARY_DIR/$PREVIOUS_VERSION/dumps

BATCH[1]="en"
BATCH[2]="fr pt it fi ru no la"
BATCH[3]="de el tr ja es bg pl nl sh sv lt mg id ku"

extract() {
  n=$1
  version=$2
  BATCH_LANGS=${BATCH[$n]}
  echo $BATCH_LANGS
  EFFECTIVE_LANGS=(`echo $BATCH_LANGS $LANGS | tr ' ' '\n' | sort | uniq -d`)
  echo "Effectively extracting languages : " $BATCH_LANGS "//" $LANGS "-->" ${EFFECTIVE_LANGS[@]}
  FEATURES="--endolex=ontolex,morphology,lime,etymology,enhancement,statistics --exolex=ontolex"
  # Extract data using PR version
  if [[ x$EFFECTIVE_LANGS != x ]]
  then
    "$BINDIR/$NEXT_VERSION/bin/dbnary" update --dir "$WIKTIONARY_DIR/$version/" -v --no-compress $FEATURES --sample "$SAMPLE_SIZE" ${EFFECTIVE_LANGS[@]} &
    sleep 60
  fi
}

BINDIR=out/dbnary
echo " ==== EXTRACTING WITH NEXT VERSION ===== "
extract 1 $NEXT_VERSION &
extract 2 $NEXT_VERSION  &
extract 3 $NEXT_VERSION  &
wait

echo " ==== EXTRACTING WITH PREVIOUS VERSION ===== "
extract 1 $PREVIOUS_VERSION &
extract 2 $PREVIOUS_VERSION  &
extract 3 $PREVIOUS_VERSION  &
wait
mkdir -p $DIFFS
FROM=$WIKTIONARY_DIR/$PREVIOUS_VERSION/extracts/ontolex/latest
TO=$WIKTIONARY_DIR/$NEXT_VERSION/extracts/ontolex/latest

for model in ontolex morphology etymology enhancement exolex_ontolex
do
  # compute differences
  echo ./kaiko/extractor/compute_diffs.sh -m $model -f "$FROM" -t "$TO" -d $DIFFS $LANGS
  PATH=$BINDIR/$NEXT_VERSION/bin:$PATH ./kaiko/extractor/compute_diffs.sh -m $model -f "$FROM" -t "$TO" -d $DIFFS $LANGS
done

"$BINDIR/$NEXT_VERSION/bin/dbnary-diff-summary" $DIFFS
