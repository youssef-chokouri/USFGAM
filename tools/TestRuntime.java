import java.net.URI;
import java.io.IOException;
import java.util.*;
import usfgam.runtime.ChatStories;

public class TestRuntime {
    enum Tab { CLIPS, DIRECT, PROFILE, SEARCH, FEED, NEWS, SHARE }
    public static void main(String[] args) throws Exception {
        List<Object> result = ChatStories.tabs(Arrays.asList(Tab.values()));
        if (!result.equals(Arrays.asList(Tab.FEED, Tab.DIRECT))) throw new AssertionError(result);
        if (!ChatStories.tabs(Arrays.asList(Tab.DIRECT, Tab.FEED)).equals(result))
            throw new AssertionError("Stories must stay first regardless of native tab order");
        try { ChatStories.tabs(Arrays.asList(Tab.FEED)); throw new AssertionError("Missing Direct accepted"); }
        catch (IllegalStateException expected) { }
        for (String path : Arrays.asList("/api/v1/feed/reels_tray/", "/api/v1/feed/reels_media_stream/",
                "/api/v1/direct_v2/inbox/", "/api/v1/media/123/info/", "/api/v1/clips/item/"))
            ChatStories.checkRequest(new URI("https://i.instagram.com" + path));
        for (String path : Arrays.asList("/api/v1/discover/topical_explore/", "/api/v1/discover/topical_explore_stream/")) {
            try { ChatStories.checkRequest(new URI("https://i.instagram.com" + path)); throw new AssertionError(path); }
            catch (IOException expected) { }
        }
        ChatStories.checkRequest(null);
        System.out.println("PASS: navigation identity/order, missing-tab rejection, Stories/DM/shared-media requests, Explore blocking");
    }
}
