#!/bin/bash
set -e

# =======================
# 1. Java Setup
# =======================
echo "Checking Java version..."
current_ver=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo "Current detected version: $current_ver"

is_valid_version() {
    [[ "$1" =~ ^21.* ]] || [[ "$1" =~ ^2[2-9].* ]]
}

if is_valid_version "$current_ver"; then
    echo "Java version is compatible."
else
    echo "Java version incompatible or missing (Need 21+). Setting up Java 21..."
    
    # Try standard paths first
    if [[ -d "/usr/lib/jvm/java-21-openjdk-amd64" ]]; then
        export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
    elif [[ -d "/usr/lib/jvm/msopenjdk-21-amd64" ]]; then
        export JAVA_HOME="/usr/lib/jvm/msopenjdk-21-amd64"
    else
        echo "Installing OpenJDK 21..."
        sudo apt-get update -qq
        sudo apt-get install -y openjdk-21-jdk-headless
        export JAVA_HOME="/usr/lib/jvm/java-21-openjdk-amd64"
    fi
    export PATH="$JAVA_HOME/bin:$PATH"
fi

echo "Using Java: $(java -version 2>&1 | grep version)"

# =======================
# 2. Android SDK Setup
# =======================
export ANDROID_HOME="$HOME/android-sdk"
export PATH="$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools"

if [ ! -d "$ANDROID_HOME/cmdline-tools" ]; then
    echo "Android SDK not found. Installing..."
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    
    echo "Downloading Command Line Tools..."
    wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
    unzip -q cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    rm cmdline-tools.zip
    
    echo "Accepting licenses..."
    yes | sdkmanager --licenses > /dev/null
    
    echo "Installing SDK components..."
    # Components matched from build.gradle
    sdkmanager "platform-tools" \
               "platforms;android-36" \
               "build-tools;36.1.0" \
               "ndk;27.3.13750724" \
               "cmake;3.22.1"

    # Install system cmake as fallback (often newer than SDK default)
    if command -v sudo &> /dev/null; then
         echo "Installing system CMake..."
         sudo apt-get install -y cmake build-essential
    fi

    echo "Checking CMake version..."
    if command -v cmake &> /dev/null; then
        cmake --version
    else
        echo "CMake not found in PATH"
    fi
else
    echo "Android SDK found at $ANDROID_HOME"
fi

# Ensure local.properties exists
echo "sdk.dir=$ANDROID_HOME" > local.properties

# =======================
# 3. Build Setup
# =======================

# Generate dummy google-services.json if missing
if [ ! -f "TMessagesProj_App/google-services.json" ]; then
    echo "Creating dummy google-services.json..."
    cat > TMessagesProj_App/google-services.json <<EOF
{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "mock-project-id",
    "storage_bucket": "mock-project-id.appspot.com"
  },
  "client": [
    {
      "client_info": {
        "mobilesdk_app_id": "1:000000000000:android:0000000000000000",
        "android_client_info": {
          "package_name": "org.telegram.messenger.regular"
        }
      },
      "oauth_client": [],
      "api_key": [
        {
          "current_key": "mock-api-key"
        }
      ],
      "services": {
        "analytics_service": {
          "status": 1
        },
        "appinvite_service": {
          "status": 1,
          "other_platform_oauth_client": []
        },
        "ads_service": {
          "status": 2
        }
      }
    }
  ],
  "configuration_version": "1"
}
EOF
fi

# =======================
# 4. Build
# =======================
echo "Starting optimized ARM64 build..."

# IMPORTANT: Limit parallelism for 8GB RAM environment
# Limit CMake native build jobs
export CMAKE_BUILD_PARALLEL_LEVEL=2 

chmod +x gradlew
# --max-workers=2 limits Gradle parallelism
# Force JVM args via command line to ensure they are picked up
./gradlew assemblePlay -x lint -x test --max-workers=2 -Dorg.gradle.jvmargs="-Xmx2048m -XX:MaxMetaspaceSize=512m"
