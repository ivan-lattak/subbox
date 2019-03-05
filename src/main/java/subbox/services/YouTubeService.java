package subbox.services;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.VideoListResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface YouTubeService {

    @NotNull
    PlaylistItemListResponse getPlaylistItems(@NotNull String uploadPlaylistId, @Nullable String pageToken);

    @NotNull
    VideoListResponse getVideosForIds(@NotNull String... videoIds);

    @NotNull
    Optional<Channel> getChannelForId(@NotNull String channelId);

}
