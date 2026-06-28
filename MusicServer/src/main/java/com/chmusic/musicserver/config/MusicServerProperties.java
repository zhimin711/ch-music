package com.chmusic.musicserver.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "music")
public class MusicServerProperties {
    private final Storage storage = new Storage();
    private final Auth auth = new Auth();
    private final Cors cors = new Cors();

    public Storage getStorage() {
        return storage;
    }

    public Auth getAuth() {
        return auth;
    }

    public Cors getCors() {
        return cors;
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
}
