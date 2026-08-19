#!/bin/bash
set -e
SDK="${ANDROID_HOME:-$HOME/android-sdk}"
BT=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
PLAT=$(ls -d "$SDK"/platforms/android-3* | sort -V | tail -1)/android.jar
OUT=Harvester.apk

mkdir -p gen obj dex

"$BT/aapt2" compile --dir res -o res.zip
"$BT/aapt2" link -o base.apk -I "$PLAT" --manifest AndroidManifest.xml --java gen \
    --min-sdk-version 26 --target-sdk-version 35 res.zip

javac -source 8 -target 8 -bootclasspath "$PLAT" -d obj $(find src gen -name '*.java')

"$BT/d8" --release --lib "$PLAT" --min-api 26 --output dex $(find obj -name '*.class')

cd dex && zip -q ../base.apk classes.dex && cd ..

"$BT/zipalign" -f 4 base.apk aligned.apk

[ -f hk.keystore ] || keytool -genkeypair -keystore hk.keystore -alias h -keyalg RSA \
    -keysize 2048 -validity 10000 -storepass hunter2 -dname "CN=Test" -keypass hunter2

"$BT/apksigner" sign --ks hk.keystore --ks-pass pass:hunter2 --key-pass pass:hunter2 \
    --out "$OUT" aligned.apk

"$BT/apksigner" verify --verbose "$OUT"
echo "BUILD OK: $OUT"
