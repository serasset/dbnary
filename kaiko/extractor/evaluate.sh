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

BINDIR=out/dbnary
echo " ==== EXTRACTING WITH NEXT VERSION ===== "
for n in 1 2 3
do
  BATCH_LANGS=${BATCH[$n]}
  echo $BATCH_LANGS
  EFFECTIVE_LANGS=(`echo $BATCH_LANGS $LANGS | tr ' ' '\n' | sort | uniq -d`)
  echo "Effectively extracting languages : " $BATCH_LANGS "//" $LANGS "-->" ${EFFECTIVE_LANGS[@]}
  FEATURES="--endolex=ontolex,morphology,lime,etymology,enhancement,statistics --exolex=ontolex"
  # Extract data using Target branch version
  if [[ x$EFFECTIVE_LANGS != x ]]
  then
    "$BINDIR/$NEXT_VERSION/bin/dbnary" update --dir "$WIKTIONARY_DIR/$NEXT_VERSION/" -v --no-compress $FEATURES --sample "$SAMPLE_SIZE" ${EFFECTIVE_LANGS[@]} &
    sleep 60
  fi
done

echo " ==== EXTRACTING WITH PREVIOUS VERSION ===== "
for n in 1 2 3
do
  BATCH_LANGS=${BATCH[$n]}
  echo $BATCH_LANGS
  EFFECTIVE_LANGS=(`echo $BATCH_LANGS $LANGS | tr ' ' '\n' | sort | uniq -d`)
  echo "Effectively extracting languages : " $BATCH_LANGS "//" $LANGS "-->" ${EFFECTIVE_LANGS[@]}
  FEATURES="--endolex=ontolex,morphology,lime,etymology,enhancement,statistics --exolex=ontolex"
  # Extract data using PR version
  if [[ x$EFFECTIVE_LANGS != x ]]
  then
    "$BINDIR/$PREVIOUS_VERSION/bin/dbnary" update --dir "$WIKTIONARY_DIR/$PREVIOUS_VERSION/" -v --no-compress $FEATURES --sample "$SAMPLE_SIZE" ${EFFECTIVE_LANGS[@]}&
    sleep 60
  fi
done
mkdir -p $DIFFS

for model in ontolex morphology etymology enhancement exolex_ontolex
do
  # compute differences
  echo ./kaiko/extractor/compute_diffs.sh -m $model -f "$WIKTIONARY_DIR/$PREVIOUS_VERSION/" -t "$WIKTIONARY_DIR/$NEXT_VERSION/" -d $DIFFS $LANGS
  PATH=$BINDIR/$NEXT_VERSION/bin:$PATH ./kaiko/extractor/compute_diffs.sh -m $model -f "$WIKTIONARY_DIR/$PREVIOUS_VERSION/" -t "$WIKTIONARY_DIR/$NEXT_VERSION/" -d $DIFFS $LANGS
done

./kaiko/cicd/compute_differences.sh