package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SharedAlbumTest {
    private JSONArray page(int start,int end,String token,int total) throws Exception {
        JSONArray entries=new JSONArray();
        for(int i=start;i<end;i++) {
            JSONArray entry=new JSONArray().put("synthetic-"+i)
                    .put(new JSONArray().put("https://lh3.googleusercontent.com/test"+i).put(4000).put(3000));
            entry.put(9,i<68 ? new JSONObject().put("76647426",new JSONArray()) : new JSONObject());
            entries.put(entry);
        }
        JSONArray meta=new JSONArray(); meta.put(21,total);
        return new JSONArray().put(JSONObject.NULL).put(entries).put(token==null ? JSONObject.NULL : token).put(meta);
    }
    private String html(JSONArray page) {
        return "AF_initDataCallback({key: 'ds:1', hash: '2', data:"+page+", sideChannel: {}});"
                + "snAcKc:{request: [\"fake_album\",null,null,\"fake_key\"]}";
    }
    private String batch(JSONArray page) {
        return ")]}'\n\n123\n"+new JSONArray().put(new JSONArray().put("wrb.fr").put("snAcKc").put(page.toString()))+"\n";
    }
    @Test public void allPagesAndAllPhotosBeyondFirst300() throws Exception {
        AtomicInteger calls=new AtomicInteger();
        SharedAlbum.Album album=SharedAlbum.fetch("https://photos.app.goo.gl/synthetic",(url,body) -> {
            int call=calls.getAndIncrement();
            if(call==0) return html(page(0,300,"next_1",746));
            assertTrue(body.startsWith("f.req="));
            return call==1 ? batch(page(300,600,"next_2",746)) : batch(page(600,746,null,746));
        });
        assertEquals(3,calls.get()); assertEquals(3,album.pages); assertEquals(746,album.total);
        assertEquals(678,album.photos.size()); assertEquals(68,album.videos);
        assertEquals("synthetic-745",album.photos.get(677).id);
    }
    @Test(expected=java.io.IOException.class) public void truncatedListingIsRejected() throws Exception {
        SharedAlbum.fetch("https://photos.google.com/share/synthetic",(url,body)->html(page(0,100,null,746)));
    }
    @Test(expected=java.io.IOException.class) public void repeatedTokenIsRejected() throws Exception {
        SharedAlbum.fetch("https://photos.google.com/share/synthetic",(url,body)->body==null
                ? html(page(0,100,"same",746)) : batch(page(100,200,"same",746)));
    }
    @Test(expected=java.io.IOException.class) public void blockedSharingIsRejected() throws Exception {
        SharedAlbum.fetch("https://photos.app.goo.gl/synthetic",(url,body)->"Please sign in");
    }
    @Test(expected=java.io.IOException.class) public void arbitraryHostsAreRejected() throws Exception {
        SharedAlbum.validate(new java.net.URL("https://example.com/photo.jpg"),true);
    }
    @Test(expected=java.io.IOException.class) public void cleartextIsRejected() throws Exception {
        SharedAlbum.validate(new java.net.URL("http://photos.google.com/share/test"),false);
    }
    @Test(expected=java.io.IOException.class) public void alternatePortIsRejected() throws Exception {
        SharedAlbum.validate(new java.net.URL("https://photos.google.com:8080/share/test"),false);
    }
    @Test public void emptyAlbumIsACompleteSnapshot() throws Exception {
        SharedAlbum.Album album=SharedAlbum.fetch("https://photos.google.com/share/synthetic",(url,body)->html(page(0,0,null,0)));
        assertTrue(album.photos.isEmpty()); assertEquals(0,album.total);
    }
}
