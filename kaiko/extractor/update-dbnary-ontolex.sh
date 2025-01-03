#!/bin/sh

PATH=$HOME/local/graalvm/bin:/home/linuxbrew/.linuxbrew/bin:$HOME/bin:/usr/local/bin:/usr/bin:/bin:/usr/local/games:/usr/games:/opt/puppetlabs/bin:/opt/virtuoso-opensource/bin
if [ ! -t 0 ]; then
  exec 1>> /var/log/dbnary/dbnary.log 2>&1
fi

DIR=${DBNARY_DIR:-$HOME/develop/wiktionary}
LANGS="fr en de pt it fi ru el tr ja es bg pl nl sh sv lt no mg id la ku zh ga ca da"
#TLANGS="fra,eng,por,deu,ell,rus,ita,fin,tur,jpn"
MIRROR=https://dumps.wikimedia.org/
#MIRROR=http://dumps.wikimedia.your.org/
#MIRROR=http://wikipedia.c3sl.ufpr.br/
#MIRROR=ftp://ftpmirror.your.org/pub/wikimedia/dumps/
OPTIONS="--tdb -v "
JVM_OPTIONS="$JVM_OPTS -Xmx80g -Xms12g"
TMPDIR=${DBNARY_TMP:-$HOME/var/tmp/}

# Change tmp dir on debian systems (as the default /tmp partition may not be sufficient to cope with TDBs)
if [ -f "/etc/debian_version" ]; then
  JVM_OPTIONS="${JVM_OPTIONS} -Djava.io.tmpdir=${TMPDIR}"
fi

if [ $# -ge 1 ]
then
  LANGS=$@
fi

  echo "==============================================="
  echo -n "  Updating and extracting DBnary dumps - "
  date 
  echo "==============================================="
  set -x
  JAVA_OPTS="$JVM_OPTIONS" dbnary update $OPTIONS --dir $DIR -s $MIRROR -k 1 --compress --endolex=ontolex,morphology,etymology,lime,statistics,enhancement --exolex=ontolex,lime $LANGS
  set +x
  
  echo "==============================================="
  echo -n "  DBnary dumps updated - "
  date 
  echo "==============================================="

  rsync -avz $DIR/extracts/ serasset@lig-getalp.imag.fr:/opt/dbnary/static/

  echo "==============================================="
  echo -n "  Correctly uploaded dumps to web - "
  date 
  echo "==============================================="

