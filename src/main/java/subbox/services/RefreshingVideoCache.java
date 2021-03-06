package subbox.services;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.Video;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import subbox.model.PlaylistMetadata;
import subbox.util.DurationFormatter;
import subbox.util.MoreExecutors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

@Service
public class RefreshingVideoCache implements VideoCache {

    @NotNull
    private static final Logger log = LoggerFactory.getLogger(RefreshingVideoCache.class);

    private static Duration evictionThreshold;
    private static Duration updatePeriod;

    @NotNull
    private final YouTubeService youTubeService;
    @NotNull
    private final ConcurrentHashMap<String, PlaylistMetadata> metadataCache = new ConcurrentHashMap<>();
    @NotNull
    private final LoadingCache<String, Future<List<Video>>> playlistCache;

    private ExecutorService loadExecutor;
    private ScheduledFuture<?> evictAndRefreshTask;

    @Autowired
    public RefreshingVideoCache(@NotNull YouTubeService youTubeService) {
        this.youTubeService = youTubeService;
        this.playlistCache = Caffeine.newBuilder()
                .build(playlistId -> loadExecutor.submit(() -> this.youTubeService.getVideos(playlistId)));
    }

    @Value("${subbox.cache.eviction-threshold}")
    public void setEvictionThreshold(@NotNull Duration evictionThreshold) {
        RefreshingVideoCache.evictionThreshold = evictionThreshold;
    }

    @Value("${subbox.cache.update-period}")
    public void setUpdatePeriod(@NotNull Duration updatePeriod) {
        RefreshingVideoCache.updatePeriod = updatePeriod;
    }

    @PostConstruct
    public void init() {
        log.info("Initializing load thread pool");
        loadExecutor = MoreExecutors.newBoundedCachedThreadPool(32);

        log.info("Initializing evictAndRefresh task");
        evictAndRefreshTask = Executors.newSingleThreadScheduledExecutor()
                .scheduleAtFixedRate(this::evictAndRefresh, updatePeriod.toNanos(), updatePeriod.toNanos(), NANOSECONDS);
    }

    @PreDestroy
    public void destroy() {
        loadExecutor.shutdown();
        log.info("Cancelling evictAndRefresh task");
        evictAndRefreshTask.cancel(false);
        try {
            log.info("Waiting for the load thread pool to die");
            loadExecutor.awaitTermination(1, MINUTES);
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    @NotNull
    public Future<List<List<Video>>> get(@NotNull List<String> playlistIds) {
        updateMetadataCache(playlistIds);
        return allOf(playlistCache.getAll(playlistIds).values());
    }

    private void updateMetadataCache(@NotNull List<String> playlistIds) {
        List<String> playlistIdsToDownload = filterAbsentIds(playlistIds);
        getMetadata(playlistIdsToDownload);
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

    private void getMetadata(@NotNull List<String> playlistIds) {
        List<Playlist> downloadedPlaylists = youTubeService.getPlaylists(playlistIds);
        for (int i = 0; i < playlistIds.size(); i++) {
            String playlistETag = downloadedPlaylists.get(i).getEtag();
            PlaylistMetadata metadata = new PlaylistMetadata(playlistETag);
            metadataCache.put(playlistIds.get(i), metadata);
        }
    }

    private void evictAndRefresh() {
        ZonedDateTime start = ZonedDateTime.now();
        log.debug("evictAndRefresh: evicting invalid and expired entries");
        log.debug("evictAndRefresh: {} metadata and {} playlists present before eviction", metadataCache.size(), playlistCache.estimatedSize());

        MutableInt evictedMetadata = new MutableInt();
        MutableInt evictedPlaylists = new MutableInt();
        metadataCache.entrySet()
                .removeIf(entry -> {
                    if (entry.getValue().isOlderThan(evictionThreshold) ||
                            playlistCache.getIfPresent(entry.getKey()) == null) {
                        evictedMetadata.increment();
                        return true;
                    }
                    return false;
                });
        playlistCache.asMap()
                .keySet()
                .removeIf(playlistId -> {
                    if (!metadataCache.containsKey(playlistId)) {
                        evictedPlaylists.increment();
                        return true;
                    }
                    return false;
                });

        log.debug("evictAndRefresh: evicted {} metadata and {} playlists", evictedMetadata, evictedPlaylists);
        log.debug("evictAndRefresh: {} metadata and {} playlists present after eviction", metadataCache.size(), playlistCache.estimatedSize());
        log.debug("evictAndRefresh: refreshing stale playlists");

        MutableInt refreshedPlaylists = new MutableInt();
        List<Playlist> playlists = youTubeService.getPlaylists(new ArrayList<>(metadataCache.keySet()));
        for (Playlist playlist : playlists) {
            String id = playlist.getId();
            PlaylistMetadata cachedPlaylist = metadataCache.get(id);
            if (Objects.equals(cachedPlaylist.getETag(), playlist.getEtag())) {
                continue;
            }

            refreshedPlaylists.increment();
            cachedPlaylist.setETag(playlist.getEtag());
            playlistCache.refresh(id);
        }

        log.debug("evictAndRefresh: refreshed {} playlists", refreshedPlaylists);
        log.debug("evictAndRefresh: finished in {}", DurationFormatter.format(Duration.between(start, ZonedDateTime.now())));
    }

    @NotNull
    private static <V> Future<List<V>> allOf(Collection<? extends Future<? extends V>> futures) {
        return new CompositeFuture<>(futures);
    }

    private static class CompositeFuture<V> implements Future<List<V>> {
        private final Collection<? extends Future<? extends V>> futures;

        CompositeFuture(Collection<? extends Future<? extends V>> futures) {
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
