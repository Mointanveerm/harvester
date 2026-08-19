package com.example.harvester;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class HarvestJob extends JobService {
    @Override
    public boolean onStartJob(final JobParameters p) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    HarvestEngine.run(getApplicationContext());
                } catch (Throwable t) {
                    /* fail silently */
                }
                jobFinished(p, false);
            }
        }).start();

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters p) {
        return true;  // retry next cycle if killed
    }
}
