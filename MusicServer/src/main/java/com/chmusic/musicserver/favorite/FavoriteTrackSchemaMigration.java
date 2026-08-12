package com.chmusic.musicserver.favorite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
class FavoriteTrackSchemaMigration {
    private static final Logger log = LoggerFactory.getLogger(FavoriteTrackSchemaMigration.class);

    @Bean
    ApplicationRunner allowExternalFavoriteTracks(JdbcTemplate jdbcTemplate) {
        return args -> {
            try {
                jdbcTemplate.execute("ALTER TABLE favorite_tracks ALTER COLUMN music_id DROP NOT NULL");
            } catch (Exception ex) {
                log.debug("favorite_tracks.music_id is already nullable or not ready for migration", ex);
            }
        };
    }
}
