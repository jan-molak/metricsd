#!/usr/bin/env bash

cd "$(dirname "$0")/.."

SCRIPT_NAME=`basename $0`
((!$#)) && echo "Specify the Project Version, for example: ./"$SCRIPT_NAME" 1.2.3-build.5.sha.c67a8c2" && exit 1

mvn versions:set -DnewVersion=$1