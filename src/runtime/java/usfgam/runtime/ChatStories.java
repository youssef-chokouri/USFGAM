package usfgam.runtime;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public final class ChatStories {
    private ChatStories() {}

    public static List<Object> tabs(List<?> candidates) {
        Object home = null, direct = null;
        for (Object tab : candidates) {
            if (!(tab instanceof Enum<?>)) continue;
            String name = ((Enum<?>) tab).name();
            if (name.equals("FEED")) home = tab;
            if (name.equals("DIRECT")) direct = tab;
        }
        if (home == null || direct == null)
            throw new IllegalStateException("Instagram navigation no longer provides FEED and DIRECT");
        List<Object> result = new ArrayList<>(2);
        result.add(home);
        result.add(direct);
        return result;
    }

    public static void checkRequest(URI uri) throws IOException {
        if (uri == null || uri.getPath() == null) return;
        String path = uri.getPath();
        if (path.startsWith("/api/v1/discover/topical_explore")
                || path.startsWith("/api/v1/discover/topical_explore_stream"))
            throw new IOException("Explore disabled by Chat and Stories patch");
    }
}
