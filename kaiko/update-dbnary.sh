#!/bin/sh

DIR=$HOME/dev/wiktionary
LANGS="fr en de pt it fi ru el tr ja es bg pl nl sh sv lt no mg id la"
TLANGS="fra,eng,por,deu,ell,rus,ita,fin,tur,jpn"
JAVA=/usr/java/jdk8/bin/java
VERS=2.1-SNAPSHOT
MIRROR=http://dumps.wikimedia.org/
#MIRROR=http://dumps.wikimedia.your.org/
#MIRROR=http://wikipedia.c3sl.ufpr.br/
#MIRROR=ftp://ftpmirror.your.org/pub/wikimedia/dumps/
EXTRACTOR=dbnary-extractor
ENHANCER=dbnary-enhancer

if [ $# -ge 1 ]
then
  LANGS=$@
fi

$JAVA  -cp $HOME/.m2/repository/org/getalp/${EXTRACTOR}/$VERS/${EXTRACTOR}-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.cli.UpdateAndExtractDumps -d $DIR -m ontolex -s $MIRROR -k 1 -z --enable morpho --enable etymology $LANGS

# Enhancing translation (source disambiguation)
$JAVA  -cp $HOME/.m2/repository/org/getalp/${ENHANCER}/$VERS/${ENHANCER}-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.enhancer.EnhanceLatestExtracts -d ${DIR}/extracts -z

# Updating latest extractions stats
$JAVA  -cp $HOME/.m2/repository/org/getalp/${EXTRACTOR}/$VERS/${EXTRACTOR}-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.cli.UpdateLatestStatistics  -d $DIR/extracts -c $TLANGS

# Updating archived extraction stats
for lg in $LANGS
do
    $JAVA -cp $HOME/.m2/repository/org/getalp/${EXTRACTOR}/$VERS/${EXTRACTOR}-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.cli.UpdateDiachronicStatistics -d $DIR/extracts -c $TLANGS $lg
done

#cd /home/serasset/bin/parrot/
#$JAVA -jar parrot-jar-with-dependencies.jar -i http://kaiko.getalp.org/dbnary -o /home/serasset/dev/wiktionary/extracts/lemon/dbnary-doc/index.html -t html/dbnarytemplate.vm -s report/css/custom.css -b ./

rsync -avz $DIR/extracts/ serasset@lig-getalp.imag.fr:/opt/dbnary/static/

