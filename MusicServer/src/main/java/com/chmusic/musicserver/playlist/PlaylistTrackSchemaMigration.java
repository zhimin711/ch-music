package com.chmusic.musicserver.playlist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class PlaylistTrackSchemaMigration {
    private static final Logger log = LoggerFactory.getLogger(PlaylistTrackSchemaMigration.class);

    @Bean
    ApplicationRunner allowExternalPlaylistTracks(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE playlist_tracks ALTER COLUMN music_id DROP NOT NULL");
            } catch (Exception ex) {
                log.debug("playlist_tracks.music_id is already nullable or not ready for migration", ex);
            }
        };
    }
}
