#!/bin/sh

PATH=$HOME/bin:$HOME/.sdkman/candidates/maven/current/bin:$HOME/.sdkman/candidates/java/current/bin:/usr/local/bin:/usr/bin:/bin:/usr/local/games:/usr/games:/opt/puppetlabs/bin:/opt/virtuoso-opensource/bin

if [ ! -t 0 ]; then
  exec 1>> /var/log/dbnary/dbnary.log 2>&1
fi

DIR=$HOME/develop/wiktionary
LANGS="fr en de pt it fi ru el tr ja es bg pl nl sh sv lt no mg id la"
TLANGS="fra,eng,por,deu,ell,rus,ita,fin,tur,jpn"
JAVA=java
VERS=2.2.2
MIRROR=http://dumps.wikimedia.org/
#MIRROR=http://dumps.wikimedia.your.org/
#MIRROR=http://wikipedia.c3sl.ufpr.br/
#MIRROR=ftp://ftpmirror.your.org/pub/wikimedia/dumps/
EXTRACTOR=dbnary-extractor
ENHANCER=dbnary-enhancer
OPTIONS="--tdb -v"

# Change tmp dir on debian systems (as the default /tmp partition may niot be sufficient to cope with TDBs)
if [ -f "/etc/debian_version" ]; then
  JVM_OPTIONS="-Djava.io.tmpdir=/var/tmp/"
fi

if [ $# -ge 1 ]
then
  LANGS=$@
fi

  echo "==============================================="
  echo -n "  Updating DBnary dumps - "
  date 
  echo "==============================================="

  $JAVA $JVM_OPTIONS -cp $HOME/.m2/repository/org/getalp/${EXTRACTOR}/$VERS/${EXTRACTOR}-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.cli.UpdateAndExtractDumps $OPTIONS -d $DIR -m ontolex -s $MIRROR -k 1 -z --enable morpho --enable etymology --enable lime $LANGS

  echo "==============================================="
  echo -n "  DBnary dumps updated - "
  date 
  echo "==============================================="

  # Enhancing translation (source disambiguation)
  $JAVA $JVM_OPTIONS -cp $HOME/.m2/repository/org/getalp/${ENHANCER}/$VERS/${ENHANCER}-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.enhancer.EnhanceLatestExtracts -d ${DIR}/extracts -z

  echo "==============================================="
  echo -n "  DBnary dumps - enhanced "
  date 
  echo "==============================================="

  # Updating latest extractions stats
  $JAVA $JVM_OPTIONS -cp $HOME/.m2/repository/org/getalp/${EXTRACTOR}/$VERS/${EXTRACTOR}-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.cli.UpdateLatestStatistics  -d $DIR/extracts -c $TLANGS

  # Updating archived extraction stats
  for lg in $LANGS
  do
    $JAVA -cp $HOME/.m2/repository/org/getalp/${EXTRACTOR}/$VERS/${EXTRACTOR}-${VERS}-jar-with-dependencies.jar org.getalp.dbnary.cli.UpdateDiachronicStatistics -d $DIR/extracts -c $TLANGS $lg
  done

  echo "==============================================="
  echo -n "  Stats updated -  "
  date 
  echo "==============================================="

  #cd /home/serasset/bin/parrot/
  #$JAVA -jar parrot-jar-with-dependencies.jar -i http://kaiko.getalp.org/dbnary -o /home/serasset/dev/wiktionary/extracts/lemon/dbnary-doc/index.html -t html/dbnarytemplate.vm -s report/css/custom.css -b ./

  rsync -avz $DIR/extracts/ serasset@lig-getalp.imag.fr:/opt/dbnary/static/

  echo "==============================================="
  echo -n "  Correctly uploaded dumps to web - "
  date 
  echo "==============================================="
