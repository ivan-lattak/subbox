package subbox.model;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.ZonedDateTime;

public class PlaylistMetadata {

    @NotNull
    private ZonedDateTime lastAccessed = ZonedDateTime.now();
    @NotNull
    private String eTag;

    public PlaylistMetadata(@NotNull String eTag) {
        this.eTag = eTag;
    }

    public void accessed() {
        lastAccessed = ZonedDateTime.now();
    }

    public boolean isOlderThan(@NotNull Duration duration) {
        Duration age = Duration.between(lastAccessed, ZonedDateTime.now());
        return age.compareTo(duration) > 0;
    }

    @NotNull
    public String getETag() {
        return eTag;
    }

    public void setETag(@NotNull String eTag) {
        this.eTag = eTag;
    }

}
