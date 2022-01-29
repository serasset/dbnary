#!/bin/sh

PATH=$HOME/bin:$HOME/.sdkman/candidates/maven/current/bin:$HOME/.sdkman/candidates/java/current/bin:/usr/local/bin:/usr/bin:/bin:/usr/local/games:/usr/games:/opt/puppetlabs/bin:/opt/virtuoso-opensource/bin

if [ ! -t 0 ]; then
  exec 1>> /var/log/dbnary/dbnary.log 2>&1
fi

DIR=${DBNARY_DIR:-$HOME/develop/wiktionary}
LANGS="fr en de pt it fi ru el tr ja es bg pl nl sh sv lt no mg id la ku"
#TLANGS="fra,eng,por,deu,ell,rus,ita,fin,tur,jpn"
JAVA=java
VERS=3.0.0b4-SNAPSHOT
MIRROR=http://dumps.wikimedia.org/
#MIRROR=http://dumps.wikimedia.your.org/
#MIRROR=http://wikipedia.c3sl.ufpr.br/
#MIRROR=ftp://ftpmirror.your.org/pub/wikimedia/dumps/
EXTRACTOR=dbnary-commands
OPTIONS="--tdb -v"

# Change tmp dir on debian systems (as the default /tmp partition may not be sufficient to cope with TDBs)
if [ -f "/etc/debian_version" ]; then
  JVM_OPTIONS="-Djava.io.tmpdir=/var/tmp/"
fi

if [ $# -ge 1 ]
then
  LANGS=$@
fi

  echo "==============================================="
  echo -n "  Updating and extracting DBnary dumps - "
  date 
  echo "==============================================="

  $JAVA $JVM_OPTIONS -cp $HOME/.m2/repository/org/getalp/${EXTRACTOR}/$VERS/${EXTRACTOR}-${VERS}-uber-jar.jar org.getalp.dbnary.cli.DBnary update $OPTIONS --dir $DIR -s $MIRROR -k 1 --compress --endolex=ontolex,morphology,etymology,lime,statistics,enhancement,combined --exolex=ontolex,combined $LANGS
  echo "==============================================="
  echo -n "  DBnary dumps updated - "
  date 
  echo "==============================================="

  rsync -avz $DIR/extracts/ serasset@lig-getalp.imag.fr:/opt/dbnary/static/

  echo "==============================================="
  echo -n "  Correctly uploaded dumps to web - "
  date 
  echo "==============================================="

