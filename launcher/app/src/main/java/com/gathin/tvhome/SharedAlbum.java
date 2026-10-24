package com.gathin.tvhome;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Read-only, anonymous shared-page adapter. No login cookies, Library API, analytics, or executable imports.
 * Protocol research: vikas5914/google-photos-album-image-url-fetch, see THIRD_PARTY_NOTICES.md.
 * This is an undocumented web format; fail closed on incomplete pagination instead of deleting the cache.
 */
final class SharedAlbum {
    static final Pattern BOOTSTRAP = Pattern.compile(
            "AF_initDataCallback\\(\\{key: 'ds:1'.*?data:(.*?), sideChannel:", Pattern.DOTALL);
    static final Pattern REQUEST = Pattern.compile(
            "snAcKc[^}]*?request:\\s*\\[\\s*\"([A-Za-z0-9_-]+)\"\\s*,\\s*null\\s*,\\s*null\\s*,\\s*\"([A-Za-z0-9_-]+)\"", Pattern.DOTALL);
    static final class Photo {
        final String id, url;
        Photo(String id, String url) { this.id = id; this.url = url; }
    }
    static final class Album {
        final List<Photo> photos;
        final int total, videos, pages;
        Album(List<Photo> photos, int total, int videos, int pages) {
            this.photos = photos; this.total = total; this.videos = videos; this.pages = pages;
        }
    }
    interface Transport { String read(String url, String body) throws Exception; }

    static Album fetch(String shareUrl, Transport transport) throws Exception {
        validate(new URL(shareUrl), false);
        String html = transport.read(shareUrl, null);
        Matcher boot = BOOTSTRAP.matcher(html);
        if (!boot.find()) throw new IOException("Album page format changed or sharing is unavailable");
        JSONArray data = new JSONArray(boot.group(1));
        JSONArray metadata = data.getJSONArray(3);
        int expected = metadata.getInt(21);
        if (expected < 0) throw new IOException("Invalid album size");
        Matcher request = REQUEST.matcher(html);
        boolean hasRequest = request.find();
        String albumKey = hasRequest ? request.group(1) : "";
        String authKey = hasRequest ? request.group(2) : "";
        LinkedHashMap<String, JSONArray> media = new LinkedHashMap<>();
        Set<String> tokens = new HashSet<>();
        int pages = 0;
        while (true) {
            pages++;
            JSONArray entries = data.optJSONArray(1);
            if (entries == null && expected != 0) throw new IOException("Missing album entries");
            if (entries != null) for (int i = 0; i < entries.length(); i++) {
                JSONArray entry = entries.getJSONArray(i);
                String id = entry.getString(0);
                if (id.isEmpty()) throw new IOException("Missing media identity");
                media.put(id, entry);
            }
            // Android's org.json and the build JVM's org.json differ in NULL string coercion.
            Object cursor = data.opt(2);
            if (cursor != null && cursor != JSONObject.NULL && !(cursor instanceof String))
                throw new IOException("Invalid album continuation token");
            String token = cursor instanceof String ? (String) cursor : "";
            if (token.isEmpty()) break;
            if (!hasRequest || !tokens.add(token) || pages >= 1000)
                throw new IOException("Incomplete album pagination; previous cache retained");
            JSONArray inner = new JSONArray().put(albumKey).put(token).put(JSONObject.NULL).put(authKey);
            JSONArray envelope = new JSONArray().put(new JSONArray().put(new JSONArray()
                    .put("snAcKc").put(inner.toString()).put(JSONObject.NULL).put("generic")));
            String body = "f.req=" + URLEncoder.encode(envelope.toString(), "UTF-8");
            data = batch(transport.read("https://photos.google.com/u/0/_/PhotosUi/data/batchexecute?rpcids=snAcKc", body));
        }
        if (media.size() != expected) throw new IOException("Album size changed or page missing; retrying later");
        List<Photo> photos = new ArrayList<>();
        int videos = 0;
        for (JSONArray entry : media.values()) {
            JSONObject attributes = entry.optJSONObject(9);
            if (attributes != null && attributes.has("76647426")) { videos++; continue; }
            JSONArray image = entry.getJSONArray(1);
            String url = image.getString(0);
            validate(new URL(url), true);
            if (image.getInt(1) <= 0 || image.getInt(2) <= 0) throw new IOException("Invalid image dimensions");
            photos.add(new Photo(entry.getString(0), url + "=w1920-h1080"));
        }
        return new Album(photos, expected, videos, pages);
    }

    static JSONArray batch(String text) throws Exception {
        for (String line : text.split("\n")) {
            if (!line.trim().startsWith("[[")) continue;
            JSONArray outer = new JSONArray(line);
            for (int i = 0; i < outer.length(); i++) {
                JSONArray row = outer.optJSONArray(i);
                if (row != null && "wrb.fr".equals(row.optString(0)) && "snAcKc".equals(row.optString(1)))
                    return new JSONArray(row.getString(2));
            }
        }
        throw new IOException("Invalid album page response");
    }

    static void validate(URL url, boolean image) throws IOException {
        String host = url.getHost();
        boolean allowed = image ? "lh3.googleusercontent.com".equals(host)
                : "photos.app.goo.gl".equals(host) || "photos.google.com".equals(host);
        if (!"https".equals(url.getProtocol()) || !allowed || url.getUserInfo() != null
                || (url.getPort() != -1 && url.getPort() != 443)) throw new IOException("Untrusted photo URL");
    }

    static byte[] download(String address, String body, boolean image, int limit) throws Exception {
        URL url = new URL(address);
        for (int redirects = 0; redirects <= 5; redirects++) {
            if (Thread.currentThread().isInterrupted()) throw new IOException("Sync interrupted");
            validate(url, image);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15_000); connection.setReadTimeout(20_000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (AuroraTV photo display)");
            try {
                if (body != null) {
                    connection.setRequestMethod("POST"); connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                    connection.setRequestProperty("Origin", "https://photos.google.com");
                    try (java.io.OutputStream output = connection.getOutputStream()) {
                        output.write(body.getBytes(StandardCharsets.UTF_8));
                    }
                }
                int code = connection.getResponseCode();
                if (code >= 300 && code <= 399) {
                    url = new URL(url, connection.getHeaderField("Location"));
                    if (body != null) throw new IOException("Unexpected pagination redirect");
                    continue;
                }
                if (code != 200) throw new IOException("Photo service HTTP " + code);
                if (connection.getContentLengthLong() > limit) throw new IOException("Photo response too large");
                try (InputStream input = connection.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[16384]; int n;
                    while ((n = input.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted()) throw new IOException("Sync interrupted");
                        if (output.size() + n > limit) throw new IOException("Photo response too large");
                        output.write(buffer, 0, n);
                    }
                    return output.toByteArray();
                }
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many photo redirects");
    }
}
