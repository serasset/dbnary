#!/bin/bash

SEDSCRIPT="~/develop/dbnary/kaiko/fix_stats.sed"
SAVEDIR="$HOME/stats.saved/`date +%F_%H-%M-%S`/"
DATADIR="$HOME/develop/wiktionary/extracts/ontolex"
mkdir -p $SAVEDIR
rsync -a $DATADIR/ $SAVEDIR/

pushd $DATADIR
for lg in ?? do
	pushd $lg
	# iterate over all dates...
	for f in ${lg}_dbanry_stat*.ttl.bz2 do
		bzcat $f | sed -f ${SEDSCRIPT} | bzip2 > $f.new
		mv $f $f.old
		mv $f.new $f
	done
	popd
done

