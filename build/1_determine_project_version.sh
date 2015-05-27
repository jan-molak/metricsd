#!/usr/bin/env bash

cd "$(dirname "$0")/.."

POM_VERSION=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v -e "^\[INFO\]" -e "Download" | cut -d- -f1`
GIT_SHORT_HASH=${GIT_COMMIT:0:7}

PROJECT_VERSION="$POM_VERSION-build.$BUILD_NUMBER.sha.$GIT_SHORT_HASH"

echo PROJECT_VERSION=$PROJECT_VERSION