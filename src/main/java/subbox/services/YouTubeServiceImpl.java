package subbox.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.VideoListResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import subbox.SubBoxApplication;
import subbox.util.Exceptions;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;

@Service
public class YouTubeServiceImpl implements YouTubeService {

    private static final long MAX_RESULTS = 50;
    @NotNull
    private static final NetHttpTransport HTTP_TRANSPORT = getHttpTransport();
    @NotNull
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    @NotNull
    private static NetHttpTransport getHttpTransport() {
        try {
            return GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private YouTube youTube;

    @NotNull
    private YouTube getYoutube() {
        if (youTube == null) {
            youTube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, request -> {})
                    .setYouTubeRequestInitializer(new YouTubeRequestInitializer(SubBoxApplication.getProperty("subbox.api.key")))
                    .setApplicationName(SubBoxApplication.getProperty("subbox.app.name"))
                    .build();
        }
        return youTube;
    }

    @NotNull
    @Override
    public PlaylistItemListResponse getPlaylistItems(@NotNull String uploadPlaylistId, @Nullable String pageToken) {
        return Exceptions.wrapIOException(() -> getYoutube()
                .playlistItems()
                .list("contentDetails")
                .setPlaylistId(uploadPlaylistId)
                .setPageToken(pageToken)
                .setMaxResults(MAX_RESULTS)
                .execute());
    }

    @NotNull
    @Override
    public VideoListResponse getVideosForIds(@NotNull String... videoIds) {
        String commaSeparatedVideoIds = String.join(",", videoIds);
        return Exceptions.wrapIOException(() -> getYoutube()
                .videos()
                .list("snippet")
                .setId(commaSeparatedVideoIds)
                .execute());
    }

    @NotNull
    @Override
    public Optional<Channel> getChannelForId(@NotNull String channelId) {
        return Exceptions.wrapIOException(() -> getYoutube()
                .channels()
                .list("contentDetails")
                .setId(channelId)
                .setMaxResults(1L)
                .execute()
                .getItems()
                .stream()
                .findFirst());
    }
}
