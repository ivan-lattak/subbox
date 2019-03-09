package subbox.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;
import subbox.model.PlaylistMetadata;
import subbox.util.MoreExecutors;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Function;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

public class RefreshingVideoCache {

    @NotNull
    private static final Duration EVICTION_THRESHOLD = Duration.of(1, DAYS);

    @NotNull
    private final ExecutorService LOAD_EXECUTOR = MoreExecutors.newBoundedCachedThreadPool(16);

    @NotNull
    private final ConcurrentHashMap<String, PlaylistMetadata> metadataCache = new ConcurrentHashMap<>();
    @NotNull
    private final LoadingCache<String, Future<List<Video>>> delegateCache;

    @NotNull
    private final Function<List<String>, List<Playlist>> bulkPlaylistDownloader;
    @NotNull
    private final Function<String, List<Video>> videoDownloader;

    public RefreshingVideoCache(@NotNull Function<List<String>, List<Playlist>> bulkPlaylistDownloader,
                                @NotNull Function<String, List<Video>> videoDownloader) {
        this.bulkPlaylistDownloader = bulkPlaylistDownloader;
        this.videoDownloader = videoDownloader;

        this.delegateCache = Caffeine.newBuilder()
                .build(playlistId -> LOAD_EXECUTOR.submit(() -> this.videoDownloader.apply(playlistId)));

        ScheduledExecutorService updateAndEvictExecutor = Executors.newSingleThreadScheduledExecutor();
        updateAndEvictExecutor.scheduleAtFixedRate(this::updateAndEvict, 0, 1, MINUTES);
    }

    @NotNull
    public Future<List<List<Video>>> get(@NotNull List<String> playlistIds) {
        updateMetadataCache(playlistIds);
        return allOf(new ArrayList<>(delegateCache.getAll(playlistIds).values()));
    }

    private void updateMetadataCache(@NotNull List<String> playlistIds) {
        List<String> playlistIdsToDownload = filterAbsentIds(playlistIds);
        downloadPlaylists(playlistIdsToDownload);
    }

    @NotNull
    private List<String> filterAbsentIds(@NotNull List<String> playlistIds) {
        List<String> absentPlaylistIds = new ArrayList<>();
        for (String playlistId : playlistIds) {
            PlaylistMetadata metadata = metadataCache.get(playlistId);
            if (metadata == null) {
                absentPlaylistIds.add(playlistId);
            } else {
                metadata.accessed();
            }
        }
        return absentPlaylistIds;
    }

    private void downloadPlaylists(@NotNull List<String> playlistIds) {
        List<Playlist> downloadedPlaylists = bulkPlaylistDownloader.apply(playlistIds);
        for (int i = 0; i < playlistIds.size(); i++) {
            String playlistETag = downloadedPlaylists.get(i).getEtag();
            PlaylistMetadata metadata = new PlaylistMetadata(playlistETag);
            metadataCache.put(playlistIds.get(i), metadata);
        }
    }

    private void updateAndEvict() {
        metadataCache.entrySet()
                .removeIf(entry ->
                        entry.getValue().isOlderThan(EVICTION_THRESHOLD) ||
                                delegateCache.getIfPresent(entry.getKey()) == null);
        delegateCache.asMap().keySet().removeIf(playlistId -> !metadataCache.containsKey(playlistId));

        List<Playlist> playlists = bulkPlaylistDownloader.apply(new ArrayList<>(metadataCache.keySet()));
        for (Playlist playlist : playlists) {
            String id = playlist.getId();
            PlaylistMetadata cachedPlaylist = metadataCache.get(id);
            if (Objects.equals(cachedPlaylist.getETag(), playlist.getEtag())) {
                continue;
            }

            cachedPlaylist.setETag(playlist.getEtag());
            delegateCache.refresh(id);
        }
    }

    @NotNull
    private static <V> Future<List<V>> allOf(List<? extends Future<? extends V>> futures) {
        return new CompositeFuture<>(futures);
    }

    private static class CompositeFuture<V> implements Future<List<V>> {
        private final List<? extends Future<? extends V>> futures;

        CompositeFuture(List<? extends Future<? extends V>> futures) {
            this.futures = futures;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return futures.stream().anyMatch(Future::isCancelled);
        }

        @Override
        public boolean isDone() {
            return futures.stream().allMatch(Future::isDone);
        }

        @Override
        public List<V> get() throws InterruptedException, ExecutionException {
            if (isCancelled()) {
                throw new CancellationException();
            }
            List<V> result = new ArrayList<>();
            for (Future<? extends V> future : futures) {
                result.add(future.get());
            }
            return result;
        }

        @Override
        public List<V> get(long timeout, @NotNull TimeUnit unit) {
            throw new UnsupportedOperationException("get(long, TimeUnit)");
        }
    }

}
