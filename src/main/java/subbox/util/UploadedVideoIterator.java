package subbox.util;

import com.google.api.services.youtube.model.*;
import subbox.services.YouTubeService;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class UploadedVideoIterator implements Iterator<Video> {

    private final YouTubeService youTubeService;
    private final String channelId;

    private String uploadPlaylistId = null;
    private List<Video> videos = null;
    private String nextPageToken = null;
    private boolean hasNextPage = true;
    private int nextVideo = 0;

    public UploadedVideoIterator(YouTubeService youTubeService, String channelId) {
        this.youTubeService = youTubeService;
        this.channelId = channelId;
    }

    @Override
    public boolean hasNext() {
        if (videos == null) {
            getFirstPage();
        }

        if (nextVideo >= videos.size() && hasNextPage) {
            getNextPage();
        }
        return nextVideo < videos.size();
    }

    @Override
    public Video next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        Video video = videos.get(nextVideo);
        nextVideo++;
        return video;
    }

    private void getFirstPage() {
        uploadPlaylistId = getUploadPlaylistId(channelId);
        getNextPage();
    }

    private String getUploadPlaylistId(String channelId) {
        return youTubeService.getChannelForId(channelId)
                .map(Channel::getContentDetails)
                .map(ChannelContentDetails::getRelatedPlaylists)
                .map(ChannelContentDetails.RelatedPlaylists::getUploads)
                .orElseThrow(IllegalStateException::new);
    }

    private void getNextPage() {
        PlaylistItemListResponse response = youTubeService.getPlaylistItems(uploadPlaylistId, nextPageToken);
        nextPageToken = response.getNextPageToken();
        hasNextPage = nextPageToken != null;

        String[] videoIds = response.getItems()
                .stream()
                .map(PlaylistItem::getContentDetails)
                .map(PlaylistItemContentDetails::getVideoId)
                .toArray(String[]::new);
        videos = youTubeService.getVideosForIds(videoIds)
                .getItems();
        nextVideo = 0;
    }

}
