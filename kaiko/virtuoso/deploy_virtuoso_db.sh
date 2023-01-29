#!/usr/bin/env bash

if [[ $(id -u) -ne 0 ]]; then
  echo >&2 "Please run as root"
  exit 1
fi

if [ ! -t 0 ]; then
  exec 1>> /var/log/dbnary/dbnary.log 2>&1
fi

## Parse command line options
OPTIND=1 # Reset in case getopts has been used previously in the shell.
DBNARY_USER_CONFIG_DIR="$HOME/.dbnary/"
VIRTUOSO_SERVICE_NAME="virtuoso-opensource-7"

VIRTUOSODBLOCATION=/var/lib/virtuoso-opensource-7/

function show_help() {
  echo "USAGE: $0 [-h] [-d dir] [-c config] [-s servicename]"
  echo "OPTIONS:"
  echo "      h: display this help message."
  echo "      d: use provided value as the virtuoso database location (default value = $VIRTUOSODBLOCATION)."
  echo "      c: use provided value as the configuration directory (default value = $DBNARY_USER_CONFIG_DIR)."
  echo "         This directory should contain config and virtuoso.ini.tmpl files."
  echo "      s: use provided value as the virtuoso service name (default value = $VIRTUOSO_SERVICE_NAME)."
}

while getopts "h?d:c:" opt; do
  case "$opt" in
    h | \?)
      show_help
      exit 0
      ;;
    c)
      DBNARY_USER_CONFIG_DIR=$OPTARG
      ;;
    d)
      VIRTUOSODBLOCATION=$OPTARG
      ;;
    s)
      VIRTUOSO_SERVICE_NAME=$OPTARG
      ;;
  esac
done

shift $((OPTIND - 1))

[ "$1" = "--" ] && shift

## Default values that will be overriden by configuration file
PATH=/sbin:/bin:/usr/sbin:/usr/bin:/opt/virtuoso-opensource/bin

## Read values from configuration file
[[ -f $DBNARY_USER_CONFIG_DIR/config ]] && source $DBNARY_USER_CONFIG_DIR/config

# exit when any command fails
set -e


function stop_virtuoso() {
  systemctl stop ${VIRTUOSO_SERVICE_NAME}.service
  sleep 15
}

function start_virtuoso() {
  systemctl start ${VIRTUOSO_SERVICE_NAME}.service
  sleep 15
}

function fix_owner() {
  for d in db.*; do
    chmod go+rx $d
    chown -R root $d
  done
}

function db_file_with_suffix() {
  suffix=$1
  file=""
  for d in db.*.${suffix} ; do
    [ -e "$d" ] && file="$d"
    break
  done
  echo ${file}
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
  nextfile=$(db_file_with_suffix next)
  previous=$(db_file_with_suffix previous)
  current=$(db_file_with_suffix current)
  target=${nextfile%.next}.current

  CURRENTDATETIMESTAMP=$(date +"%Y-%m-%d-%H-%M-%S")
  if [[ ! -z ${previous} ]]
  then
    mv ${previous} db.delete.${CURRENTDATETIMESTAMP}
  fi
  mv ${current} ${current%.current}.previous
  mv ${nextfile} ${target}
  ln -s ${target} db

  rm -rf db.delete.${CURRENTDATETIMESTAMP}
}

cd "$VIRTUOSODBLOCATION"
nextfile=$(db_file_with_suffix next)
if [ "x${nextfile}" = "x" ]; then
  >&2 echo Deploy Virtuoso DB : No next database available to deploy. Exiting.
  exit
fi

# keep track of the last executed command
trap 'last_command=$current_command; current_command=$BASH_COMMAND' DEBUG
# echo an error message before exiting
trap 'echo "\"${last_command}\" command failed with exit code $?."' EXIT

stop_virtuoso
fix_owner
rotate_and_link
start_virtuoso

trap - EXIT
