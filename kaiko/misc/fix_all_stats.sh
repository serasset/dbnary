#!/bin/bash

SEDSCRIPT="$HOME/develop/dbnary/kaiko/misc/$1"
if [[ ! -f $SEDSCRIPT ]] ; then
	>&2 echo "usage : fix_all_stats.sh sed-file-name"
	exit 1
fi

SAVEDIR="$HOME/stats.saved/`date +%F_%H-%M-%S`/"
DATADIR="$HOME/develop/wiktionary/extracts/ontolex"
mkdir -p $SAVEDIR
rsync --include='??' --include='??/??_dbnary_stat*.ttl.bz2' --exclude='*.ttl*' -a $DATADIR/ $SAVEDIR/

pushd $DATADIR
for lg in ??; do
	pushd $lg
	# iterate over all dates...
	for f in ${lg}_dbnary_stat*.ttl.bz2 ; do
		bzcat $f | sed -f ${SEDSCRIPT} | bzip2 > $f.new
		mv $f $f.old
		mv $f.new $f
	done
	popd
done

