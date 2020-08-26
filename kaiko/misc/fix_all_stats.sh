#!/bin/bash

SEDSCRIPT=~/develop/dbnary/kaiko/misc/fix_stats.sed
pushd "$HOME/develop/wiktionary/extracts/ontolex"
mkdir ~/stats.saved
cp "./??/??_dbnary_stat*" ~/stats.saved
for lg in ??; do
  echo ${lg}
  pushd "${lg}"
  # iterate over all dates...
  for f in ${lg}_dbnary_stat*.ttl.bz2; do
    echo $f
    bzcat "$f" | sed -f "${SEDSCRIPT}" | bzip2 > "$f.new"
  done
  popd
done

