#!/bin/bash

SEDSCRIPT=~/develop/dbnary/kaiko/
pushd $HOME/develop/wiktionary/extracts/ontolex
mkdir ~/stats.saved
cp ??/??_dbnary_stat* ~/stats.saved
for lg in ?? do
	pushd $lg
	# iterate over all dates...
	for f in ${lg}_dbanry_stat*.ttl.bz2 do
		bzcat $f | sed -f ${SEDSCRIPT} | bzip2 > $f.new
	done
	popd
done

