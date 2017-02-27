#!/bin/bash

DIR=$HOME/wiktionary/extracts/lemon/latest
LANGS="fr en de pt it fi ru el tr ja es bg pl nl sh sv lt no mg id la"
JAVA=java
VERS=1.0-SNAPSHOT

if [[ $# -gt 0 ]]
then
    LANGS=$@
fi

for lg in $LANGS
do
    $JAVA  -cp $HOME/.m2/repository/org/getalp/dbnary/experiment/trans2links-experiment/$VERS/trans2links-experiment-${VERS}-jar-with-dependencies.jar -Dorg.slf4j.simpleLogger.log.org.getalp.dbnary=debug org.getalp.dbnary.experiment.TranslationSourcesTarget -g -c conf-${lg}.csv -s stats-${lg}.csv -l ${lg} -t $HOME/tdb/ ${DIR}/${lg}_dbnary_lemon.ttl
done



