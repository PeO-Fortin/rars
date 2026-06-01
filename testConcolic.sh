#!/bin/sh
set -e

mkdir -p build

JUNIT=junit-platform-console-standalone-1.12.2.jar

# Compilation du src
find src -name "*.java" | \
xargs javac --release 17 \
-d build \
-cp .:/usr/share/java/com.microsoft.z3.jar:lib/jsoftfloat.jar:$JUNIT

# Compilation des tests
find test -name "*.java" | \
xargs javac --release 17 \
-d build \
-cp build:/usr/share/java/com.microsoft.z3.jar:lib/jsoftfloat.jar:$JUNIT

# Copier les ressources non-java
find src -type f -not -name "*.java" -exec cp --parents {} build \;

cp -rf build/src/* build/

# Lancer les tests
java -jar $JUNIT \
  --class-path build \
  --select-class rars.concolic.ConcolicUnitTesting