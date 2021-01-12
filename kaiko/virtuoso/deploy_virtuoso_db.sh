#!/usr/bin/env bash

if [[ $(id -u) -ne 0 ]]; then
  echo "Please run as root"
  exit 1
fi
## Parse command line options
OPTIND=1 # Reset in case getopts has been used previously in the shell.

VIRTUOSODBLOCATION=/var/lib/virtuoso-opensource-7/

function show_help() {
  echo "USAGE: $0 [-h] [-d dir]"
  echo "OPTIONS:"
  echo "      h: display this help message."
  echo "      d: use provided value as the virtuoso database location (default value = $VIRTUOSODBLOCATION)."
}

while getopts "h?d:" opt; do
  case "$opt" in
  h | \?)
    show_help
    exit 0
    ;;
  d)
    VIRTUOSODBLOCATION=$OPTARG
    ;;
  esac
done

shift $((OPTIND - 1))

[ "$1" = "--" ] && shift

# exit when any command fails
set -e

# keep track of the last executed command
trap 'last_command=$current_command; current_command=$BASH_COMMAND' DEBUG
# echo an error message before exiting
trap 'echo "\"${last_command}\" command filed with exit code $?."' EXIT

function stop_virtuoso() {
  systemctl stop virtuoso-opensource-7.service
  sleep 15
}

function start_virtuoso() {
  systemctl start virtuoso-opensource-7.service
  sleep 15
}

function fix_owner() {
  for d in db.*; do
    chmod go+rx $d
    chown -R root $d
  done
}

function rotate_and_link() {
  if [ -d "db" ]
  then
    if [ -L db ]; then
      rm db
    else
      echo >&2 db is not a link, please fix directory layout in ${VIRTUOSODBLOCATION}
      exit
    fi
  fi
  nextfile=""
  for d in db.*.next; do
    nextfile="$d"
    break
  done
  previous=""
  for d in db.*.previous; do
    previous="$d"
    break
  done
  current=""
  for d in db.*.current; do
    current="$d"
    break
  done
  target=${next%.next}.current
  mv ${previous} db.delete
  mv ${current} ${current%.current}.previous
  mv ${nextfile} ${target}
  ln -s ${target} db
}

## Default values that will be overriden by configuration file
PATH=/sbin:/bin:/usr/sbin:/usr/bin:/opt/virtuoso-opensource/bin

script_dir=$(dirname $(realpath $0))

cd "$VIRTUOSODBLOCATION"
nextfile=""
for d in db.*.next; do
  nextfile="$d"
  break
done
if [ "x${nextfile}" = "x" ]; fi
  >&2 echo No next database available to deploy. Exiting.
  exit
fi

stop_virtuoso
fix_owner
rotate_and_link
start_virtuoso
