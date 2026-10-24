package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Decodes one bounded image at a time off the UI thread. The shuffle ledger commits before presentation. */
final class FamilyPhotos {
    private final Context context;
    private final ImageView view;
    private final SharedPreferences state;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = this::update;
    private boolean active, busy, closed;
    private String showing = "";
    private final int width, height;
    FamilyPhotos(Context context, ImageView view) {
        this.context = context.getApplicationContext(); this.view = view;
        state = context.getSharedPreferences("family_photo_cycle", Context.MODE_PRIVATE);
        width = Math.min(1920, context.getResources().getDisplayMetrics().widthPixels);
        height = Math.min(1080, context.getResources().getDisplayMetrics().heightPixels);
    }
    void start() { active = true; update(); }
    void stop() { active = false; handler.removeCallbacks(tick); }
    void releaseForMemoryPressure() { view.setImageDrawable(null); showing = ""; }
    void close() { closed = true; stop(); executor.shutdownNow(); }
    void update() {
        handler.removeCallbacks(tick);
        if (!active || closed) return;
        handler.postDelayed(tick, 10_000);
        if (busy) return;
        busy = true;
        executor.execute(() -> {
            List<File> files = PhotoStore.photos(context);
            List<String> eligible = new ArrayList<>();
            for (File file : files) eligible.add(file.getName());
            String source = PhotoStore.index(context).optString("source");
            boolean sameSource = source.equals(state.getString("source", ""));
            PhotoCycle cycle = new PhotoCycle(sameSource ? state.getStringSet("seen", new HashSet<>()) : new HashSet<>(), new Random());
            String previous = sameSource ? state.getString("image", "") : "";
            long changed = state.getLong("changed", 0), now = System.currentTimeMillis();
            int minutes = context.getSharedPreferences(Config.SETTINGS_NAME, Context.MODE_PRIVATE).getInt("photo_minutes", 1);
            boolean due = WallpaperRotation.due(changed, now, Math.max(1, minutes) * 60_000L);
            String candidate = eligible.contains(previous) && !due ? previous : cycle.next(eligible, previous);
            Bitmap bitmap = null;
            while (candidate != null) {
                if (candidate.equals(showing)) break;
                bitmap = decode(new File(PhotoStore.directory(context), candidate));
                if (bitmap != null) break;
                eligible.remove(candidate);
                candidate = cycle.next(eligible, previous);
            }
            String picked = candidate;
            Bitmap decoded = bitmap;
            handler.post(() -> {
                busy = false;
                if (closed || !active) { if (decoded != null) decoded.recycle(); return; }
                if (decoded == null) {
                    if (eligible.isEmpty()) { view.setImageDrawable(null); showing = ""; }
                    return;
                }
                cycle.displayed(picked);
                boolean persisted = state.edit().putString("source", source).putString("image", picked)
                        .putLong("changed", picked.equals(previous) && changed != 0 ? changed : now)
                        .putStringSet("seen", cycle.seen).commit();
                if (!persisted) {
                    decoded.recycle(); PhotoStore.status(context, "Photo history could not be saved; rotation paused"); return;
                }
                showing = picked;
                view.setImageBitmap(decoded);
                // Let rendering release the previous buffer; never recycle a bitmap still used by the GPU.
            });
        });
    }
    private Bitmap decode(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), options);
            if (options.outWidth <= 0 || options.outHeight <= 0 || options.outWidth > 4096 || options.outHeight > 4096) return null;
            options.inSampleSize = WallpaperSize.sampleSize(options.outWidth, options.outHeight, width, height);
            options.inJustDecodeBounds = false;
            Bitmap source = BitmapFactory.decodeFile(file.getPath(), options);
            if (source == null) return null;
            Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(result); canvas.drawColor(0xFF111824);
            float scale = Math.max(width / (float) source.getWidth(), height / (float) source.getHeight());
            float w = source.getWidth() * scale, h = source.getHeight() * scale;
            // Fill the display with the photo, matching the original launcher's CENTER_CROP wallpaper behavior.
            canvas.drawBitmap(source, null, new RectF((width-w)/2, (height-h)/2, (width+w)/2, (height+h)/2),
                    new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            source.recycle(); return result;
        } catch (Exception | OutOfMemoryError ignored) { return null; }
    }
}
