#!/bin/sh
set -e
CLASS="$1"
shift
./build.sh
java -Djava.library.path=/usr/lib/x86_64-linux-gnu/jni -cp build:/usr/share/java/com.microsoft.z3.jar "$CLASS" $@