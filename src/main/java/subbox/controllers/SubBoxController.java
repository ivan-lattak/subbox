package subbox.controllers;

import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import subbox.services.AsyncVideoService;
import subbox.services.UploadedVideoIterator;
import subbox.services.YouTubeService;
import subbox.util.iterators.Iterators;

import javax.validation.constraints.*;
import java.util.*;
import java.util.stream.BaseStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Validated
public class SubBoxController {

    @NotNull
    private static final Comparator<Video> BY_PUBLISH_TIME_DESC = comparingLong((Video video) ->
            video.getSnippet().getPublishedAt().getValue()).reversed();

    @Autowired
    private YouTubeService youTubeService;

    @Autowired
    private AsyncVideoService videoService;

    @NotNull
    @GetMapping("/videosOld")
    public List<Video> getVideos(@NotNull @RequestParam("channelIds") List<String> channelIds) {
        if (channelIds.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "channelIds must not be empty");
        }
        if (channelIds.stream().anyMatch(String::isBlank)) {
            throw new ResponseStatusException(BAD_REQUEST, "none of the specified channelIds may be blank");
        }

        List<Iterator<Video>> iterators = channelIds.stream()
                .map(this::getVideos)
                .map(BaseStream::iterator)
                .collect(toList());

        return mergeSorted(iterators)
                .collect(toList());
    }

    @NotNull
    @GetMapping("/videos")
    public List<Video> videos(@RequestParam("channelIds") @NotNull @NotEmpty Set<@NotBlank String> channelIds,
                              @RequestParam(value = "perPage", defaultValue = "20") @Positive @Max(50) int perPage,
                              @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero int page) {
        videoService.getUploadedVideos(new ArrayList<>(channelIds));
        return List.of();
    }

    @NotNull
    private Stream<Video> getVideos(@NotNull String channelId) {
        Iterator<Video> iterator = new UploadedVideoIterator(youTubeService, channelId);
        Spliterator<Video> spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
        return StreamSupport.stream(spliterator, false)
                .sorted(BY_PUBLISH_TIME_DESC);
    }

    @NotNull
    private Stream<Video> mergeSorted(@NotNull List<Iterator<Video>> iterators) {
        return iterators.stream()
                .reduce((i1, i2) -> Iterators.mergeSorted(i1, i2, BY_PUBLISH_TIME_DESC))
                .map(it -> Spliterators.spliteratorUnknownSize(it, 0))
                .stream()
                .flatMap(spliterator -> StreamSupport.stream(spliterator, false));
    }

}
