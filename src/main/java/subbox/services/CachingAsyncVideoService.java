package subbox.services;

import com.google.api.services.youtube.model.Channel;
import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import subbox.util.UncancellableFuture;

import java.util.Iterator;
import java.util.List;

@Service
public class CachingAsyncVideoService implements AsyncVideoService {

    @NotNull
    @Override
    public UncancellableFuture<Iterator<Video>> getUploadedVideos(@NotNull Channel channelId) {
        return null;
    }

    @NotNull
    @Override
    public List<UncancellableFuture<Iterator<Video>>> getUploadedVideos(@NotNull Channel... channelIds) {
        return null;
    }

}
