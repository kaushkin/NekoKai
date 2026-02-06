#!/bin/bash

# Function to check java version
check_java_version() {
    if type -p java > /dev/null; then
        _java=java
    elif [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]];  then
        _java="$JAVA_HOME/bin/java"
    else
        echo "no java"
        return 1
    fi

    if [[ "$_java" ]]; then
        version=$("$_java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
        echo "$version"
    fi
}

echo "Detected Java version: $(check_java_version)"

# Try to find Java 17 or higher if current is likely too low (simple heuristic)
# Gradle 9+ needs Java 17
if [[ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]]; then
    export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    echo "Setting JAVA_HOME to $JAVA_HOME"
elif [[ -d "/usr/lib/jvm/msopenjdk-17-amd64" ]]; then
    export JAVA_HOME="/usr/lib/jvm/msopenjdk-17-amd64"
    echo "Setting JAVA_HOME to $JAVA_HOME"
elif [[ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]]; then
    export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
    echo "Setting JAVA_HOME to $JAVA_HOME"
fi

# Ensure gradlew is executable
chmod +x gradlew

# Run the build
echo "Starting optimized ARM64 build..."
./gradlew assemblePlay -x lint -x test --parallel
