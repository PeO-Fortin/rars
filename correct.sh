#!/bin/sh
set -e

shift_args() {
    CLASS="$1"
    shift
    java -Djava.library.path=/usr/lib/x86_64-linux-gnu/jni \
         -cp build:/usr/share/java/com.microsoft.z3.jar:lib/jsoftfloat.jar \
         "$CLASS" "$@"
}

./build.sh

shift_args rars.concolic.Corrector "$@"