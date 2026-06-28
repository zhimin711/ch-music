package com.chmusic.musicserver.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "music")
public class MusicServerProperties {
    private final Storage storage = new Storage();
    private final Auth auth = new Auth();
    private final Cors cors = new Cors();
    private final Streaming streaming = new Streaming();
    private final Transcoding transcoding = new Transcoding();

    public Storage getStorage() {
        return storage;
    }

    public Auth getAuth() {
        return auth;
    }

    public Cors getCors() {
        return cors;
    }

    public Streaming getStreaming() {
        return streaming;
    }

    public Transcoding getTranscoding() {
        return transcoding;
    }

    public static class Storage {
        private String root = "./.local/music-storage";

        public String getRoot() {
            return root;
        }

        public void setRoot(String root) {
            this.root = root;
        }
    }

    public static class Auth {
        private int tokenTtlDays = 30;

        public int getTokenTtlDays() {
            return tokenTtlDays;
        }

        public void setTokenTtlDays(int tokenTtlDays) {
            this.tokenTtlDays = tokenTtlDays;
        }
    }

    public static class Cors {
        private String allowedOrigins = "*";

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public static class Streaming {
        private final Range range = new Range();

        public Range getRange() {
            return range;
        }
    }

    public static class Range {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Transcoding {
        private boolean enabled = false;
        private String ffmpegPath = "ffmpeg";
        private int maxConcurrency = 1;
        private String tempRoot = "./.local/transcode-temp";
        private String cacheRoot = "./.local/transcode-cache";
        private DataSize cacheMaxSize = DataSize.ofGigabytes(20);
        private List<Profile> profiles = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getFfmpegPath() {
            return ffmpegPath;
        }

        public void setFfmpegPath(String ffmpegPath) {
            this.ffmpegPath = ffmpegPath;
        }

        public int getMaxConcurrency() {
            return maxConcurrency;
        }

        public void setMaxConcurrency(int maxConcurrency) {
            this.maxConcurrency = maxConcurrency;
        }

        public String getTempRoot() {
            return tempRoot;
        }

        public void setTempRoot(String tempRoot) {
            this.tempRoot = tempRoot;
        }

        public String getCacheRoot() {
            return cacheRoot;
        }

        public void setCacheRoot(String cacheRoot) {
            this.cacheRoot = cacheRoot;
        }

        public DataSize getCacheMaxSize() {
            return cacheMaxSize;
        }

        public void setCacheMaxSize(DataSize cacheMaxSize) {
            this.cacheMaxSize = cacheMaxSize;
        }

        public List<Profile> getProfiles() {
            return profiles;
        }

        public void setProfiles(List<Profile> profiles) {
            this.profiles = profiles == null ? new ArrayList<>() : profiles;
        }
    }

    public static class Profile {
        private String id;
        private String label;
        private String contentType;
        private Integer bitrateKbps;
        private String extension;
        private String args;
        private boolean offlineCacheable = true;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public Integer getBitrateKbps() {
            return bitrateKbps;
        }

        public void setBitrateKbps(Integer bitrateKbps) {
            this.bitrateKbps = bitrateKbps;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public String getArgs() {
            return args;
        }

        public void setArgs(String args) {
            this.args = args;
        }

        public boolean isOfflineCacheable() {
            return offlineCacheable;
        }

        public void setOfflineCacheable(boolean offlineCacheable) {
            this.offlineCacheable = offlineCacheable;
        }
    }
}
