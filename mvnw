#!/bin/sh
# ----------------------------------------------------------------------------
# Maven Wrapper startup script
# ----------------------------------------------------------------------------

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Download Maven wrapper if not present
WRAPPER_JAR="$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_PROPERTIES="$SCRIPT_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$WRAPPER_JAR" ]; then
    echo "Downloading Maven wrapper..."
    WRAPPER_URL=$(grep "wrapperUrl" "$WRAPPER_PROPERTIES" | cut -d'=' -f2)
    mkdir -p "$SCRIPT_DIR/.mvn/wrapper"
    curl -sL "$WRAPPER_URL" -o "$WRAPPER_JAR"
fi

# Run Maven
exec java -jar "$WRAPPER_JAR" "$@"
