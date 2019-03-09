package subbox.services;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.ChannelContentDetails;
import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.StreamSupport;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class CachingAsyncVideoService implements AsyncVideoService {

    @Autowired
    private YouTubeService youTubeService;

    @NotNull
    private final LoadingCache<String, Optional<Channel>> channelCache = Caffeine.newBuilder()
            .expireAfterWrite(1, DAYS)
            .build(new ChannelCacheLoader());
    @NotNull
    private final RefreshingVideoCache videoCache = new RefreshingVideoCache(youTubeService::getPlaylists, youTubeService::getVideos);

    @NotNull
    @Override
    public Future<List<List<Video>>> getUploadedVideos(@NotNull List<String> channelIds) {
        Map<String, Optional<Channel>> channels = channelCache.getAll(channelIds);
        // TODO: 8.3.19 verify all channels exist

        videoCache.get(channels.values()
                .stream()
                .map(Optional::orElseThrow)
                .map(Channel::getContentDetails)
                .map(ChannelContentDetails::getRelatedPlaylists)
                .map(ChannelContentDetails.RelatedPlaylists::getUploads)
                .collect(toList()));
        return CompletableFuture.failedFuture(new Throwable());
    }

    private class ChannelCacheLoader implements CacheLoader<String, Optional<Channel>> {
        @NotNull
        @Override
        public Optional<Channel> load(@NotNull String channelId) {
            return youTubeService.getChannel(channelId);
        }

        @NotNull
        @Override
        public Map<String, Optional<Channel>> loadAll(@NotNull Iterable<? extends String> channelIds) {
            return bulkLoadChannels(listOf(channelIds));
        }

        @NotNull
        @SuppressWarnings("unchecked")
        private List<String> listOf(@NotNull Iterable<? extends String> channelIds) {
            if (channelIds instanceof List) {
                return (List<String>) channelIds;
            }
            return StreamSupport.stream(channelIds.spliterator(), false)
                    .collect(toList());
        }

        @NotNull
        private Map<String, Optional<Channel>> bulkLoadChannels(List<String> ids) {
            List<Channel> channels = youTubeService.getChannels(ids);
            return ids.stream()
                    .collect(toMap(identity(), id -> channels.stream()
                            .filter(channel -> channel.getId().equals(id))
                            .findFirst()));
        }
    }

}
