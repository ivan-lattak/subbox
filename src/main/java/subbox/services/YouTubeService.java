package subbox.services;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.Playlist;
import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static java.util.Comparator.comparingLong;

public interface YouTubeService {

    @NotNull
    Comparator<Video> DEFAULT_VIDEO_COMPARATOR = comparingLong((Video video) ->
            video.getSnippet().getPublishedAt().getValue()).reversed();

    @NotNull
    Optional<Channel> getChannel(@NotNull String channelId);

    @NotNull
    List<Channel> getChannels(@NotNull List<String> channelIds);

    @NotNull
    List<Playlist> getPlaylists(@NotNull List<String> playlistIds);

    @NotNull
    List<Video> getVideos(@NotNull String playlistId);

}
