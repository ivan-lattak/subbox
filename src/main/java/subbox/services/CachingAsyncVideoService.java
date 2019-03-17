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

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.StreamSupport;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class CachingAsyncVideoService implements AsyncVideoService {

    @NotNull
    private final LoadingCache<String, Optional<Channel>> channelCache = Caffeine.newBuilder()
            .expireAfterWrite(1, DAYS)
            .build(new ChannelCacheLoader());

    @Autowired
    private YouTubeService youTubeService;
    @Autowired
    private RefreshingVideoCache videoCache;

    @NotNull
    @Override
    public Future<List<List<Video>>> getUploadedVideos(@NotNull List<String> channelIds) {
        Map<String, Optional<Channel>> channels = channelCache.getAll(channelIds);
        checkChannelsPresent(channels);

        return videoCache.get(channels.values()
                .stream()
                .map(Optional::get)
                .map(Channel::getContentDetails)
                .map(ChannelContentDetails::getRelatedPlaylists)
                .map(ChannelContentDetails.RelatedPlaylists::getUploads)
                .collect(toList()));
    }

    private void checkChannelsPresent(Map<String, Optional<Channel>> channels) {
        List<String> nonexistentChannels = channels.entrySet()
                .stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(toList());

        if (!nonexistentChannels.isEmpty()) {
            String exceptionMessage = "Channels not found: " + String.join(", ", nonexistentChannels);
            throw new ChannelNotFoundException(exceptionMessage);
        }
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
            if (channelIds instanceof Collection) {
                return new ArrayList<>((Collection<String>) channelIds);
            }
            return StreamSupport.stream(channelIds.spliterator(), false)
                    .collect(toList());
        }

        @NotNull
        private Map<String, Optional<Channel>> bulkLoadChannels(List<String> ids) {
            Map<String, Channel> foundChannels = youTubeService.getChannels(ids)
                    .stream()
                    .collect(toMap(Channel::getId, identity()));
            return ids.stream()
                    .collect(toMap(identity(), id -> Optional.ofNullable(foundChannels.get(id))));
        }
    }

}
