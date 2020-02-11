#!/bin/bash
PROJECT_VERSION=`./gradlew -q  getProjectVersion`
tags="$PROJECT_VERSION $(echo $PROJECT_VERSION | awk -F '.' '{printf $1"."$2" "$1}')"
for t in $tags 
do
    echo "$t"
done

