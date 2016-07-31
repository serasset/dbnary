#!/bin/sh

DIR=/home/serasset/dev/wiktionary/extracts/lemon/latest
LANGS="fr en de pt it fi ru el tr ja es bg pl nl sh sv lt no mg id"
JAVA=/usr/java/jdk7/bin/java
VERS=1.0-SNAPSHOT

for lg in $LANGS
do
    $JAVA  -cp /home/serasset/.m2/repository/org/getalp/dbnary/experiment/lld2014-experiment/$VERS/lld2014-experiment-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.experiment.DisambiguateTranslationSources -c conf-fr.csv -s stats-fr.csv -l ${lg} ${DIR}/${lg}_dbnary_lemon.ttl.bz2
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


