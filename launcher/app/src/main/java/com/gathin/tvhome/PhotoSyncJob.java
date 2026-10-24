package com.gathin.tvhome;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/** Android owns the periodic job across reboot; standby/network restrictions defer it without losing history. */
public final class PhotoSyncJob extends JobService {
    private volatile Thread worker;
    static void schedule(Context context, boolean now) {
        if (!PhotoStore.source(context).isFile()) return;
        JobScheduler scheduler = context.getSystemService(JobScheduler.class);
        ComponentName service = new ComponentName(context, PhotoSyncJob.class);
        if (scheduler.getPendingJob(4101) == null) scheduler.schedule(new JobInfo.Builder(4101, service)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
                .setPeriodic(15 * 60_000L).build());
        if (now && scheduler.getPendingJob(4102) == null) scheduler.schedule(new JobInfo.Builder(4102, service)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(1).build());
    }
    @Override public boolean onStartJob(JobParameters params) {
        if (worker != null && worker.isAlive()) return false;
        worker = new Thread(() -> {
            boolean retry = false;
            try { PhotoStore.sync(this); }
            catch (Exception error) {
                if (error instanceof InterruptedException) Thread.currentThread().interrupt();
                // Never log exceptions containing URLs, tokens, photo IDs or account metadata.
                PhotoStore.status(this, "Photo sync paused; cached photos continue. Check connection, sharing or free storage.");
                retry = true;
            }
            if (!Thread.currentThread().isInterrupted()) jobFinished(params, retry);
        }, "AuroraPhotoSync");
        worker.start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        if (worker != null) worker.interrupt();
        return true;
    }
}
