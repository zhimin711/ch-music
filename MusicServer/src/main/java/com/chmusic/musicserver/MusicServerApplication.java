package com.chmusic.musicserver;

import com.chmusic.musicserver.config.MusicServerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties(MusicServerProperties.class)
public class MusicServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(MusicServerApplication.class, args);
	}

}
