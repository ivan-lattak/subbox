package subbox.services;

import com.google.api.services.youtube.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface YouTubeService {

    @NotNull
    Optional<Channel> getChannel(@NotNull String channelId);

    @NotNull
    List<Channel> getChannels(@NotNull List<String> channelIds);

    @NotNull
    List<Playlist> getPlaylists(@NotNull List<String> playlistIds);

    @NotNull
    PlaylistItemListResponse getPlaylistItems(@NotNull String uploadPlaylistId, @Nullable String pageToken);

    @NotNull
    VideoListResponse getVideos(@NotNull String... videoIds);

    @NotNull
    List<Video> getVideos(@NotNull String playlistId);

}
