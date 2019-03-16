package subbox.controllers;

import com.google.api.services.youtube.model.Video;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import subbox.services.AsyncVideoService;
import subbox.services.YouTubeService;
import subbox.util.iterators.Iterators;

import javax.validation.constraints.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

@RestController
@Validated
public class SubBoxController {

    @Autowired
    private AsyncVideoService videoService;

    @NotNull
    @GetMapping("/videos")
    public List<Video> videos(@RequestParam("channelIds") @NotNull @NotEmpty Set<@NotBlank String> channelIds,
                              @RequestParam(value = "perPage", defaultValue = "20") @Positive @Max(50) int perPage,
                              @RequestParam(value = "page", defaultValue = "0") @PositiveOrZero int page) throws ExecutionException {
        Future<List<List<Video>>> uploadedVideos = videoService.getUploadedVideos(new ArrayList<>(channelIds));
        List<List<Video>> videoLists;
        try {
            videoLists = uploadedVideos.get();
        } catch (InterruptedException e) {
            throw new Error(e);
        }

        List<Iterator<Video>> videoIterators = videoLists.stream()
                .map(List::iterator)
                .collect(toList());

        Iterator<Video> mergedIterator = Iterators.mergeSorted(videoIterators, YouTubeService.DEFAULT_VIDEO_COMPARATOR);
        Spliterator<Video> spliterator = Spliterators.spliteratorUnknownSize(mergedIterator, 0);
        return StreamSupport.stream(spliterator, false)
                .skip(perPage * page)
                .limit(perPage)
                .collect(toList());
    }

}
