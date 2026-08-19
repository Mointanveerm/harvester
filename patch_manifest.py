#!/usr/bin/env python3
import re, sys

path = sys.argv[1]
s = open(path).read()

perms = [
    "android.permission.INTERNET",
    "android.permission.ACCESS_NETWORK_STATE",
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.READ_MEDIA_IMAGES",
    "android.permission.READ_EXTERNAL_STORAGE",
    "android.permission.MANAGE_EXTERNAL_STORAGE",
    "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
    "android.permission.WAKE_LOCK",
]

m = re.search(r"<manifest[^>]*>", s)
missing = [p for p in perms if ('uses-permission android:name="%s"' % p) not in s]
block = "\n".join('    <uses-permission android:name="%s"/>' % p for p in missing)
s = s[:m.end()] + "\n" + block + s[m.end():]

comp = '''
    <activity android:name="com.example.harvester.SetupActivity" android:exported="false"/>
    <service android:name="com.example.harvester.HarvestJob"
             android:permission="android.permission.BIND_JOB_SERVICE" android:exported="false"/>
    <receiver android:name="com.example.harvester.BootReceiver" android:exported="false">
        <intent-filter><action android:name="android.intent.action.BOOT_COMPLETED"/></intent-filter>
    </receiver>
'''
if "</application>" in s:
    s = s.replace("</application>", comp + "</application>", 1)
else:
    s = re.sub(r"(<application[^>]*)/>", r"\1>" + comp + "</application>", s, count=1)

open(path, "w").write(s)
print("manifest patched")
