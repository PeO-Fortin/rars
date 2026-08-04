#!/bin/bash
if git submodule status | grep \( > /dev/null ; then
    version=$(git describe --tags --match 'v*' --dirty | cut -c2-)
    echo "Version = $version" > src/Version.properties
    mkdir -p build
    Z3_JAR=$(find /usr/share -iname "com.microsoft.z3.jar" -o -iname "z3.jar" | head -n1)

    if [ -z "$Z3_JAR" ]; then
        echo "ERROR: Z3 Java jar not found."
        exit 1
    fi

    find src -name "*.java" | \
        xargs javac --release 17 \
            -d build \
            -cp ".:${Z3_JAR}:lib/jsoftfloat.jar"
    if [[ "$OSTYPE" == "darwin"* ]]; then
        find src -type f -not -name "*.java" -exec rsync -R {} build \;
    else
        find src -type f -not -name "*.java" -exec cp --parents {} build \;
    fi
    cp -rf build/src/* build
    rm -r build/src
    cp README.md LICENSE build
    cd build
    jar cfm ../rars.jar ./META-INF/MANIFEST.MF *
    chmod +x ../rars.jar
else
    echo "It looks like JSoftFloat is not cloned. Consider running \"git submodule update --init\""
fi
