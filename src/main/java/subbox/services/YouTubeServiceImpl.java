package subbox.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import subbox.SubBoxApplication;
import subbox.util.Exceptions;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
public class YouTubeServiceImpl implements YouTubeService {

    private static final int MAX_RESULTS = 50;
    private static final long MAX_RESULTS_L = (long) MAX_RESULTS;
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

    @NotNull
    private ThreadLocal<YouTube> youTube = ThreadLocal.withInitial(() -> new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, request -> {
    })
            .setYouTubeRequestInitializer(new YouTubeRequestInitializer(SubBoxApplication.getProperty("subbox.api.key")))
            .setApplicationName(SubBoxApplication.getProperty("subbox.app.name"))
            .build());

    @NotNull
    private YouTube getYoutube() {
        return youTube.get();
    }

    @NotNull
    @Override
    public Optional<Channel> getChannel(@NotNull String channelId) {
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

    @NotNull
    @Override
    public List<Channel> getChannels(@NotNull List<String> channelIds) {
        return batches(channelIds)
                .map(batch -> Exceptions.wrapIOException(() -> getYoutube()
                        .channels()
                        .list("contentDetails")
                        .setId(String.join(",", batch))
                        .setMaxResults((long) batch.size())
                        .execute()
                        .getItems()))
                .reduce(new ArrayList<>(), (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                });
    }

    @NotNull
    @Override
    public List<Playlist> getPlaylists(@NotNull List<String> playlistIds) {
        return batches(playlistIds)
                .map(batch -> Exceptions.wrapIOException(() -> getYoutube()
                        .playlists()
                        .list("")
                        .setId(String.join(",", batch))
                        .setMaxResults((long) batch.size())
                        .execute()
                        .getItems()))
                .reduce(new ArrayList<>(), (l1, l2) -> {
                    l1.addAll(l2);
                    return l1;
                });
    }

    @NotNull
    @Override
    public PlaylistItemListResponse getPlaylistItems(@NotNull String uploadPlaylistId, @Nullable String pageToken) {
        return Exceptions.wrapIOException(() -> getYoutube()
                .playlistItems()
                .list("contentDetails")
                .setPlaylistId(uploadPlaylistId)
                .setPageToken(pageToken)
                .setMaxResults(MAX_RESULTS_L)
                .execute());
    }

    @NotNull
    @Override
    public VideoListResponse getVideos(@NotNull String... videoIds) {
        String commaSeparatedVideoIds = String.join(",", videoIds);
        return Exceptions.wrapIOException(() -> getYoutube()
                .videos()
                .list("snippet")
                .setId(commaSeparatedVideoIds)
                .execute());
    }

    @NotNull
    @Override
    public List<Video> getVideos(@NotNull String playlistId) {
        return List.of();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <T> Stream<List<T>> batches(@NotNull Collection<? extends T> source) {
        List<T> list = source instanceof List ? (List<T>) source : new ArrayList<>(source);

        int size = list.size();
        if (size == 0) {
            return Stream.empty();
        }

        int fullBatches = (size - 1) / MAX_RESULTS;
        return IntStream.rangeClosed(0, fullBatches)
                .mapToObj(n ->
                        list.subList(n * MAX_RESULTS, n == fullBatches ? size : (n + 1) * MAX_RESULTS));
    }

}
