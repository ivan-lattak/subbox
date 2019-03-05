package subbox.controllers;

import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import subbox.services.YouTubeService;
import subbox.util.MergingIterator;
import subbox.util.UploadedVideoIterator;

import java.util.*;
import java.util.stream.BaseStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;

@RestController
public class SubBoxController {

    private static final Comparator<Video> BY_PUBLISH_TIME_DESC = comparingLong((Video video) ->
            video.getSnippet().getPublishedAt().getValue()).reversed();
    @Autowired
    private YouTubeService youTubeService;

    @GetMapping("/videos")
    public List<Video> getVideos(@NotNull @RequestParam("channelIds") List<String> channelIds) {
        if (channelIds.isEmpty()) {
            throw new RuntimeException();
        }
        if (channelIds.stream().anyMatch(String::isBlank)) {
            throw new RuntimeException();
        }

        List<Iterator<Video>> iterators = channelIds.stream()
                .map(this::getVideos)
                .map(BaseStream::iterator)
                .collect(toList());

        return mergeSorted(iterators)
                .collect(toList());
    }

    private Stream<Video> mergeSorted(List<Iterator<Video>> iterators) {
        return iterators.stream()
                .reduce((i1, i2) -> new MergingIterator<>(i1, i2, BY_PUBLISH_TIME_DESC))
                .map(it -> Spliterators.spliteratorUnknownSize(it, 0))
                .stream()
                .flatMap(spliterator -> StreamSupport.stream(spliterator, false));
    }

    private Stream<Video> getVideos(String channelId) {
        Iterator<Video> iterator = new UploadedVideoIterator(youTubeService, channelId);
        Spliterator<Video> spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
        return StreamSupport.stream(spliterator, false)
                .sorted(BY_PUBLISH_TIME_DESC);
    }

}
