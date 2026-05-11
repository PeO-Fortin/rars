#!/bin/sh
set -e

mkdir -p build

find src -name "*.java" | xargs javac --release 17 -d build -cp .:/usr/share/java/com.microsoft.z3.jar
find src -type f -not -name "*.java" -exec cp --parents {} build \;
cp -rf build/src/* build/