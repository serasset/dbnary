#!/bin/bash

VERS=1.0-SNAPSHOT

java  -cp $HOME/.m2/repository/org/getalp/dbnary/experiment/trans2links-experiment/$VERS/trans2links-experiment-${VERS}-jar-with-dependencies.jar -Dorg.slf4j.simpleLogger.log.org.getalp.dbnary=debug org.getalp.dbnary.experiment.DetectHomonym $HOME/Documents/TER/wiktionary/Results_5/Graph $HOME/Documents/TER/tdb > $HOME/Documents/TER/wiktionary/Results_5/processHomonyms.txt




