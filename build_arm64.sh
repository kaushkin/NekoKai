#!/bin/bash

echo "Checking Java version..."
current_ver=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Current detected version: $current_ver"

# Helper to checking if version string starts with 17 or 21 or higher
is_valid_version() {
    [[ "$1" =~ ^17.* ]] || [[ "$1" =~ ^21.* ]] || [[ "$1" =~ ^1[89].* ]] || [[ "$1" =~ ^2[0-9].* ]]
}

if is_valid_version "$current_ver"; then
    echo "Java version is compatible."
else
    echo "Java version is too old. Searching for Java 17+..."
    
    FOUND=0
    
    # List of probable paths
    CANDIDATES=(
        "/usr/lib/jvm/java-17-openjdk-amd64"
        "/usr/lib/jvm/msopenjdk-17-amd64"
        "/usr/local/sdkman/candidates/java/17.0.13-ms"
        "/usr/local/sdkman/candidates/java/17.0.12-ms"
        "/usr/local/sdkman/candidates/java/17.0.11-ms"
        "/usr/local/sdkman/candidates/java/17.0.10-ms"
        "/usr/lib/jvm/java-1.17.0-openjdk-amd64"
    )

    # Try to find via update-java-alternatives
    if command -v update-java-alternatives &> /dev/null; then
         echo "Checking update-java-alternatives..."
         # Filter for lines containing 17 or 21, get the path (3rd column)
         ALT_PATH=$(update-java-alternatives --list | grep -E '17|21' | head -n 1 | awk '{print $3}')
         if [[ -n "$ALT_PATH" ]]; then
             CANDIDATES+=( "$ALT_PATH" )
         fi
    fi

    for TARGET in "${CANDIDATES[@]}"; do
        if [[ -d "$TARGET" ]] && [[ -x "$TARGET/bin/java" ]]; then
             VER=$("$TARGET/bin/java" -version 2>&1 | awk -F '"' '/version/ {print $2}')
             if is_valid_version "$VER"; then
                 echo "Found compatible Java ($VER) at $TARGET"
                 export JAVA_HOME="$TARGET"
                 export PATH="$JAVA_HOME/bin:$PATH"
                 FOUND=1
                 break
             fi
        fi
    done

    # If still not found, try to install
    if [[ $FOUND -eq 0 ]]; then
        echo "Compatible Java not found. Attempting to install OpenJDK 17..."
        if command -v sudo &> /dev/null && command -v apt-get &> /dev/null; then
            echo "Updating apt and installing..."
            sudo apt-get update -qq
            sudo apt-get install -y openjdk-17-jdk-headless
            
            # Set path after install
            export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
            export PATH="$JAVA_HOME/bin:$PATH"
        else
            echo "Cannot install Java (no sudo/apt). Please install Java 17 manually."
            exit 1
        fi
    fi
fi

echo "Using Java: $(java -version 2>&1 | grep version)"
echo "JAVA_HOME: $JAVA_HOME"

# Ensure gradlew is executable
chmod +x gradlew

# Run the build
echo "Starting optimized ARM64 build..."
./gradlew assemblePlay -x lint -x test --parallel
