#!/bin/sh

DIR=$HOME/dev/wiktionary
JAVA=/usr/java/jdk8/bin/java
VERS=2.1-SNAPSHOT
MODULE=dbnary-enhancer

$JAVA  -cp $HOME/.m2/repository/org/getalp/${MODULE}/$VERS/${MODULE}-${VERS}-jar-with-dependencies.jar \
org.getalp.dbnary.enhancer.DisambiguateTranslationSources -g -z -d ${DIR}/extracts/ontolex/latest/ \
-s ${DIR}/extracts/ontolex/latest/glossesStats.csv -c ${DIR}/extracts/ontolex/latest/enhancementConfidence.csv

rsync -avz $DIR/extracts/ serasset@ken-web.imag.fr:/opt/www/kaiko/static/