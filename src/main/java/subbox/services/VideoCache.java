package subbox.services;

import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.Future;

interface VideoCache {

    @NotNull
    Future<List<List<Video>>> get(@NotNull List<String> playlistIds);

}
