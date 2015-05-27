#!/usr/bin/env bash

cd "$(dirname "$0")/.."

SCRIPT_NAME=`basename $0`
((!$#)) && echo "Specify the Project Version, for example: ./"$SCRIPT_NAME" 1.2.3-build.5.sha.c67a8c2" && exit 1

# ignore changes to execution mode of build scripts; otherwise they'll show up in the commit
git config core.fileMode false
git add pom.xml
git commit -m "Release candidate $1"

git push origin +release-$1