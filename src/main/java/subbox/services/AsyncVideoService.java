package subbox.services;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;
import subbox.util.UncancellableFuture;

import java.util.Iterator;
import java.util.List;

public interface AsyncVideoService {

    @NotNull
    UncancellableFuture<Iterator<Video>> getUploadedVideos(@NotNull Channel channelId);

    @NotNull
    List<UncancellableFuture<Iterator<Video>>> getUploadedVideos(@NotNull Channel... channelIds);

}
