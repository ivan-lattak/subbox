package subbox.services;

import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Future;

public interface AsyncVideoService {

    @NotNull
    Future<List<List<Video>>> getUploadedVideos(@NotNull List<String> channelIds);

}
