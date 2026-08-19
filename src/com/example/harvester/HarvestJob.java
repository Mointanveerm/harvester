package com.example.harvester;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class HarvestJob extends JobService {
    @Override public boolean onStartJob(JobParameters p) {
        new Thread(() -> {
            try {
                HarvestEngine.run(getApplicationContext());
            } catch (Throwable t) { /* fail silently */ }
            jobFinished(p, false);
        }).start();
        return true;
    }

    @Override public boolean onStopJob(JobParameters p) {
        return true;  // retry next cycle if killed
    }
}
