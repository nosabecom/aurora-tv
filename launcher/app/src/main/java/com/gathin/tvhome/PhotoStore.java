package com.gathin.tvhome;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Files live in internal app storage (excluded from backup), including sharing credentials and membership. */
final class PhotoStore {
    static final String CHANGED = "com.nosabecom.auroratv.PHOTOS_CHANGED";
    static final String PERMISSION = "com.nosabecom.auroratv.permission.PHOTO_STATUS";
    private static final Object SYNC_LOCK = new Object();
    static File directory(Context context) { return new File(context.getFilesDir(), "photos"); }
    static File manifest(Context context) { return new File(context.getFilesDir(), "photos-index.json"); }
    static File source(Context context) { return new File(context.getFilesDir(), "photos.json"); }

    static String hash(String text) throws Exception {
        byte[] bytes = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) result.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return result.toString();
    }

    static JSONObject index(Context context) {
        try { return new JSONObject(new String(new AtomicFile(manifest(context)).readFully(), StandardCharsets.UTF_8)); }
        catch (Exception ignored) { return new JSONObject(); }
    }

    static List<File> photos(Context context) {
        JSONArray names = index(context).optJSONArray("files");
        List<File> files = new ArrayList<>();
        if (names != null) for (int i = 0; i < names.length(); i++) {
            String name = names.optString(i);
            if (!name.matches("[a-f0-9]{64}\\.jpg")) continue;
            File file = new File(directory(context), name);
            if (file.isFile() && file.length() > 0) files.add(file);
        }
        return files;
    }

    static void write(File file, byte[] bytes) throws IOException {
        AtomicFile atomic = new AtomicFile(file);
        FileOutputStream stream = atomic.startWrite();
        try { stream.write(bytes); atomic.finishWrite(stream); }
        catch (IOException error) { atomic.failWrite(stream); throw error; }
    }

    static void sync(Context context) throws Exception {
        synchronized (SYNC_LOCK) {
            File config = source(context);
            if (!config.isFile()) return;
            if (config.length() > 8192) throw new IOException("Photo configuration too large");
            String url = new JSONObject(new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8)).getString("shared_album_url");
            SharedAlbum.Album album = SharedAlbum.fetch(url, (address, body) -> new String(
                    SharedAlbum.download(address, body, false, 8 * 1024 * 1024), StandardCharsets.UTF_8));
            File directory = directory(context);
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create photo cache");
            String source = hash(url);
            JSONArray names = new JSONArray();
            Set<String> keep = new HashSet<>();
            java.util.List<SharedAlbum.Photo> missing = new ArrayList<>();
            java.util.List<String> missingNames = new ArrayList<>();
            int cached = 0;
            for (SharedAlbum.Photo photo : album.photos) {
                String name = hash(source + ":" + photo.id) + ".jpg";
                names.put(name); keep.add(name);
                File file = new File(directory, name);
                if (file.isFile() && file.length() > 0) cached++;
                else { missing.add(photo); missingNames.add(name); }
            }
            // Three bounded workers keep first sync within the OS job window, without a service or wake lock.
            java.util.concurrent.ExecutorService downloads = java.util.concurrent.Executors.newFixedThreadPool(3);
            java.util.concurrent.atomic.AtomicInteger ready = new java.util.concurrent.atomic.AtomicInteger(cached);
            java.util.concurrent.CompletionService<Void> completion = new java.util.concurrent.ExecutorCompletionService<>(downloads);
            status(context, "Syncing photos: " + cached + " / " + album.photos.size());
            try {
                for (int i = 0; i < missing.size(); i++) {
                    SharedAlbum.Photo photo = missing.get(i);
                    File target = new File(directory, missingNames.get(i));
                    completion.submit(() -> {
                        if (Thread.currentThread().isInterrupted()) throw new IOException("Sync interrupted");
                        if (directory.getUsableSpace() < 64L * 1024 * 1024)
                            throw new IOException("Photo cache needs more free storage");
                        byte[] bytes = SharedAlbum.download(photo.url, null, true, 10 * 1024 * 1024);
                        BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);
                        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth > 4096 || bounds.outHeight > 4096)
                            throw new IOException("Unreadable photo; previous cache retained");
                        if (Thread.currentThread().isInterrupted()) throw new IOException("Sync interrupted");
                        write(target, bytes);
                        int count = ready.incrementAndGet();
                        if (count % 20 == 0) status(context, "Syncing photos: " + count + " / " + album.photos.size());
                        return null;
                    });
                }
                for (int i = 0; i < missing.size(); i++) completion.take().get();
            } finally {
                downloads.shutdownNow();
                // Complete cancellation before another sync can reuse the directory's AtomicFiles.
                boolean interrupted = Thread.interrupted();
                try { downloads.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS); }
                finally { if (interrupted) Thread.currentThread().interrupt(); }
            }
            if (Thread.currentThread().isInterrupted()) throw new IOException("Sync interrupted");
            JSONObject index = new JSONObject().put("source", source).put("files", names)
                    .put("total", album.total).put("videos", album.videos).put("pages", album.pages)
                    .put("synced", System.currentTimeMillis());
            write(manifest(context), index.toString().getBytes(StandardCharsets.UTF_8));
            // Remove only media absent from a complete, validated new snapshot; never on network failure.
            File[] cacheFiles = directory.listFiles();
            if (cacheFiles != null) for (File file : cacheFiles) {
                if (file.getName().matches("[a-f0-9]{64}\\.jpg") && !keep.contains(file.getName())) file.delete();
            }
            status(context, album.photos.size() + " photos ready · " + album.videos + " videos excluded");
            context.sendBroadcast(new android.content.Intent(CHANGED).setPackage(context.getPackageName()), PERMISSION);
        }
    }

    static void status(Context context, String text) {
        context.getSharedPreferences("photo_sync", Context.MODE_PRIVATE).edit().putString("status", text).apply();
    }
    static String status(Context context) {
        return context.getSharedPreferences("photo_sync", Context.MODE_PRIVATE).getString("status",
                source(context).exists() ? "Waiting to sync photos" : "No photo album configured");
    }
}
