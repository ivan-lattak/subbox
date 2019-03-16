package subbox.services;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeRequestInitializer;
import com.google.api.services.youtube.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import subbox.SubBoxApplication;
import subbox.util.DurationFormatter;
import subbox.util.Exceptions;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
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

    private static final Logger log = LoggerFactory.getLogger(YouTubeServiceImpl.class);

    @NotNull
    private static NetHttpTransport getHttpTransport() {
        try {
            return GoogleNetHttpTransport.newTrustedTransport();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    private final ThreadLocal<YouTube> youTube = ThreadLocal.withInitial(
            () -> new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY, null)
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
                        .setFields("items(id,contentDetails/relatedPlaylists/uploads)")
                        .execute()
                        .getItems()))
                .collect(joiningLists());
    }

    @NotNull
    @Override
    public List<Playlist> getPlaylists(@NotNull List<String> playlistIds) {
        return batches(playlistIds)
                .map(batch -> Exceptions.wrapIOException(() -> getYoutube()
                        .playlists()
                        .list("id")
                        .setId(String.join(",", batch))
                        .setMaxResults((long) batch.size())
                        .setFields("items(id,etag)")
                        .execute()
                        .getItems()))
                .collect(joiningLists());
    }

    @NotNull
    private PlaylistItemListResponse getPlaylistItems(@NotNull String playlistId, @Nullable String pageToken) {
        return Exceptions.wrapIOException(() -> getYoutube()
                .playlistItems()
                .list("contentDetails")
                .setPlaylistId(playlistId)
                .setPageToken(pageToken)
                .setMaxResults(MAX_RESULTS_L)
                .setFields("nextPageToken,items/contentDetails/videoId")
                .execute());
    }

    @NotNull
    @Override
    public List<Video> getVideos(@NotNull String playlistId) {
        log.debug("Downloading playlist \"{}\"", playlistId);
        log.debug("Fetching video ids for playlist \"{}\"", playlistId);
        ZonedDateTime start = ZonedDateTime.now();

        List<String> videoIds = new ArrayList<>();
        String nextPageToken = null;
        do {
            PlaylistItemListResponse response = getPlaylistItems(playlistId, nextPageToken);
            nextPageToken = response.getNextPageToken();
            response.getItems()
                    .stream()
                    .map(PlaylistItem::getContentDetails)
                    .map(PlaylistItemContentDetails::getVideoId)
                    .forEach(videoIds::add);
        } while (nextPageToken != null);

        log.debug("Fetched video ids for playlist \"{}\", took {}", playlistId, DurationFormatter.format(Duration.between(start, ZonedDateTime.now())));
        log.debug("Downloading videos for playlist \"{}\"", playlistId);
        ZonedDateTime startDownload = ZonedDateTime.now();

        List<Video> downloadedVideos = batches(videoIds)
                .map(batch -> Exceptions.wrapIOException(() -> getYoutube()
                        .videos()
                        .list("snippet")
                        .setId(String.join(",", batch))
                        .setMaxResults((long) batch.size())
                        .setFields("items(id,snippet(channelId,publishedAt,thumbnails/default,title))")
                        .execute()
                        .getItems()))
                .collect(joiningLists());
        downloadedVideos.sort(YouTubeService.DEFAULT_VIDEO_COMPARATOR);

        log.debug("Downloaded videos for playlist \"{}\", took {}", playlistId, DurationFormatter.format(Duration.between(startDownload, ZonedDateTime.now())));
        log.debug("Downloaded playlist \"{}\", took {}", playlistId, DurationFormatter.format(Duration.between(start, ZonedDateTime.now())));
        return downloadedVideos;
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

    @NotNull
    private static <T> Collector<List<T>, ?, List<T>> joiningLists() {
        return Collector.of(ArrayList::new, List::addAll, (List<T> l1, List<T> l2) -> {
            l1.addAll(l2);
            return l1;
        });
    }

}
