#!/bin/sh

DIR=$HOME/dev/wiktionary/tmp/latest.now
LANGS="fr en de pt it fi ru el tr ja es bg pl nl sh sv lt no mg id la"
#JAVA=/usr/java/jdk7/bin/java
if [[ -z $JAVA ]]
then
    JAVA=java
fi

VERS=1.0-SNAPSHOT

if [[ $# -gt 0 ]]
then
    LANGS=$*
fi

for lg in $LANGS
do
    $JAVA -Xmx12G -cp $HOME/.m2/repository/org/getalp/dbnary/experiment/trans2links-experiment/$VERS/trans2links-experiment-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.experiment.DisambiguateTranslationSources -g -c conf-${lg}.csv -s stats-${lg}.csv -l ${lg} ${DIR}/${lg}_dbnary_ontolex.ttl
done

# Updating latest extractions stats
#$JAVA  -cp /home/serasset/.m2/repository/org/getalp/dbnary/$VERS/dbnary-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.cli.UpdateLatestStatistics  -d $DIR/extracts -c $TLANGS

# Updating archived extraction stats
#for lg in $LANGS
#do
#    $JAVA -cp /home/serasset/.m2/repository/org/getalp/dbnary/$VERS/dbnary-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.cli.UpdateDiachronicStatistics -d /home/serasset/dev/wiktionary/extracts -c $TLANGS $lg
#done

#cd /home/serasset/bin/parrot/
#$JAVA -jar parrot-jar-with-dependencies.jar -i http://kaiko.getalp.org/dbnary -o /home/serasset/dev/wiktionary/extracts/lemon/dbnary-doc/index.html -t html/dbnarytemplate.vm -s report/css/custom.css -b ./

#rsync -avz /home/serasset/dev/wiktionary/extracts/ serasset@ken-web.imag.fr:/opt/www/kaiko/static/


