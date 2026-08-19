package com.example.harvester;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;

public class Scheduler {
    public static final int JOB_ID = 24255;  // unusual number = no collision with viewer's own jobs

    public static void schedule(Context c) {
        JobScheduler js = (JobScheduler) c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(c, HarvestJob.class))
                .setPeriodic(Config.INTERVAL_HOURS * 60 * 60 * 1000L)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .setPersisted(true)          // survives reboot
                .build();
        js.schedule(job);
    }
}
