#!/data/data/com.termux/files/usr/bin/env bash
set -e

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$APP_DIR"

echo "=== Building Native Android APK ==="

# 1. Create build directories
mkdir -p build/gen build/obj build/bin libs

# 2. Download android.jar if not present
if [ ! -f libs/android.jar ]; then
    echo "[1/6] Downloading android.jar (SDK platform library)..."
    curl -sSL -o libs/android.jar "https://github.com/Sable/android-platforms/raw/master/android-30/android.jar"
fi

# 3. Generate R.java with AAPT
echo "[2/6] Generating R.java resources..."
aapt package -f -m \
    -J build/gen \
    -S res \
    -M AndroidManifest.xml \
    -I libs/android.jar

# 4. Compile Java files to bytecode
echo "[3/6] Compiling Java source files..."
javac -d build/obj \
    -classpath libs/android.jar \
    -sourcepath "src:build/gen" \
    src/com/example/myapp/MainActivity.java build/gen/com/example/myapp/R.java

# 5. Dexing bytecode to DEX
echo "[4/6] Converting bytecode to DEX using d8..."
d8 --output build/bin --classpath libs/android.jar $(find build/obj -name "*.class")

# 6. Packaging APK with AAPT
echo "[5/6] Packaging APK file..."
aapt package -f \
    -M AndroidManifest.xml \
    -S res \
    -I libs/android.jar \
    -F build/bin/app-unsigned.apk \
    build/bin

# 7. Generate debug key and sign APK
if [ ! -f debug.keystore ]; then
    echo "Generating debug keystore..."
    keytool -genkey -v \
        -keystore debug.keystore \
        -storepass android \
        -alias androiddebugkey \
        -keypass android \
        -keyalg RSA \
        -keysize 2048 \
        -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US"
fi

echo "[6/6] Signing APK with apksigner..."
apksigner sign \
    --ks debug.keystore \
    --ks-pass pass:android \
    --key-pass pass:android \
    --out app-release.apk \
    build/bin/app-unsigned.apk

echo "=== BUILD SUCCESSFUL ==="
echo "APK location: $APP_DIR/app-release.apk"
ls -lh app-release.apk
