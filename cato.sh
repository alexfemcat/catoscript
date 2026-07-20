#!/usr/bin/env bash
# cato: launcher for the catoscript CLI.
# Usage: cato [run|compile] <file.cato>
#        cato <file.cato>          (defaults to run)

set -e

if [ "$#" -eq 0 ] || [ "$#" -gt 2 ]; then
    echo "usage: cato [run|compile] <file.cato>" >&2
    exit 2
fi

if [ "$#" -eq 2 ]; then
    if [ "$1" != "run" ] && [ "$1" != "compile" ]; then
        echo "usage: cato [run|compile] <file.cato>" >&2
        exit 2
    fi
    MODE="$1"
    SCRIPT_FILE="$2"
else
    MODE="run"
    SCRIPT_FILE="$1"
fi

if [ ! -f "$SCRIPT_FILE" ]; then
    echo "cato: file not found: $SCRIPT_FILE" >&2
    exit 1
fi

VERSION="0.3.2-LOCAL"
ARTIFACT_DIR="${HOME}/.m2/repository/com/catoscript/catoscript/${VERSION}"
JAR="${ARTIFACT_DIR}/catoscript-${VERSION}.jar"

if [ ! -f "$JAR" ]; then
    echo "cato: artifact not found at $JAR" >&2
    echo "cato: run './gradlew publishToMavenLocal' first" >&2
    exit 1
fi

DEPS_DIR="${HOME}/.m2/repository"
KOTLIN_JARS=$(find "${DEPS_DIR}/org/jetbrains/kotlin" -name 'kotlin-stdlib-*.jar' -o -name 'kotlin-stdlib-jdk8-*.jar' -o -name 'kotlin-stdlib-jdk7-*.jar' -o -name 'kotlin-stdlib-common-*.jar' 2>/dev/null | tr '\n' ':')
SERIALIZATION_JARS=$(find "${DEPS_DIR}/org/jetbrains/kotlinx" -name '*.jar' 2>/dev/null | tr '\n' ':')

CP="${JAR}:${KOTLIN_JARS}${SERIALIZATION_JARS}"

exec java -cp "$CP" com.catoscript.cli.RunScriptKt "$MODE" "$SCRIPT_FILE"
