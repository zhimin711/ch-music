package com.chmusic.musicserver.music;

import com.chmusic.musicserver.config.MusicServerProperties;
import com.chmusic.musicserver.user.AppUser;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StreamingService {
    private static final String BYTES_UNIT = "bytes";
    private static final String ORIGINAL_PROFILE = "original";

    private final MusicService musicService;
    private final TranscodeService transcodeService;
    private final TranscodeProfileCatalog profileCatalog;
    private final MusicServerProperties properties;

    public StreamingService(MusicService musicService, TranscodeService transcodeService,
            TranscodeProfileCatalog profileCatalog, MusicServerProperties properties) {
        this.musicService = musicService;
        this.transcodeService = transcodeService;
        this.profileCatalog = profileCatalog;
        this.properties = properties;
    }

    public ResponseEntity<?> streamOriginal(AppUser owner, Long musicId, HttpHeaders requestHeaders) {
        StreamResource stream = resolveOriginal(owner, musicId);
        if (!properties.getStreaming().getRange().isEnabled()) {
            return fullResponse(stream);
        }

        List<HttpRange> ranges;
        try {
            ranges = parseRanges(requestHeaders);
        } catch (IllegalArgumentException ex) {
            return unsatisfiableResponse(stream.contentLength());
        }
        if (ranges.isEmpty()) {
            return fullResponse(stream);
        }
        if (ranges.size() > 1) {
            return unsatisfiableResponse(stream.contentLength());
        }
        return partialResponse(stream, ranges.get(0));
    }

    public ResponseEntity<?> streamVariant(AppUser owner, Long musicId, String profileId, HttpHeaders requestHeaders) {
        if (profileId == null || profileId.isBlank() || ORIGINAL_PROFILE.equals(profileId)) {
            return streamOriginal(owner, musicId, requestHeaders);
        }
        StreamResource stream = resolveVariant(owner, musicId, profileId);
        if (!properties.getStreaming().getRange().isEnabled()) {
            return fullResponse(stream);
        }

        List<HttpRange> ranges;
        try {
            ranges = parseRanges(requestHeaders);
        } catch (IllegalArgumentException ex) {
            return unsatisfiableResponse(stream.contentLength());
        }
        if (ranges.isEmpty()) {
            return fullResponse(stream);
        }
        if (ranges.size() > 1) {
            return unsatisfiableResponse(stream.contentLength());
        }
        return partialResponse(stream, ranges.get(0));
    }

    public StreamResource resolveOriginal(AppUser owner, Long musicId) {
        MusicFile music = musicService.requireOwnedMusic(owner, musicId);
        Resource resource = musicService.stream(owner, musicId);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored music file is missing");
        }
        return new StreamResource(music, resource, ORIGINAL_PROFILE, music.getContentType(), music.getFileSize());
    }

    public StreamResource resolveVariant(AppUser owner, Long musicId, String profileId) {
        MusicFile music = musicService.requireOwnedMusic(owner, musicId);
        TranscodeProfileCatalog.ProfileDescriptor profile = profileCatalog.find(profileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcode profile not found"));
        Path path = transcodeService.requireReadyVariant(owner, musicId, profileId);
        Resource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcoded file is missing");
        }
        try {
            return new StreamResource(music, resource, profile.id(), contentTypeOrDefault(profile.contentType()),
                    Files.size(path));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transcoded file is missing", ex);
        }
    }

    private ResponseEntity<Resource> fullResponse(StreamResource stream) {
        return ResponseEntity.ok()
                .headers(commonHeaders(stream))
                .contentLength(stream.contentLength())
                .body(stream.resource());
    }

    private ResponseEntity<Resource> partialResponse(StreamResource stream, HttpRange range) {
        ResourceRegion region;
        try {
            region = range.toResourceRegion(stream.resource());
        } catch (IllegalArgumentException ex) {
            return unsatisfiableResponse(stream.contentLength());
        }

        long start = region.getPosition();
        long count = region.getCount();
        long end = start + count - 1;
        if (start < 0 || count <= 0 || end >= stream.contentLength()) {
            return unsatisfiableResponse(stream.contentLength());
        }
        Resource regionResource = regionResource(stream.resource(), start, count);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .headers(commonHeaders(stream))
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + stream.contentLength())
                .contentLength(count)
                .body(regionResource);
    }

    private ResponseEntity<Resource> unsatisfiableResponse(long contentLength) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.ACCEPT_RANGES, BYTES_UNIT)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                .build();
    }

    private HttpHeaders commonHeaders(StreamResource stream) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT_RANGES, BYTES_UNIT);
        headers.setContentType(MediaType.parseMediaType(stream.contentType()));
        headers.setContentDisposition(ContentDisposition.inline()
                .filename(stream.music().getOriginalFilename(), StandardCharsets.UTF_8)
                .build());
        return headers;
    }

    private static List<HttpRange> parseRanges(HttpHeaders requestHeaders) {
        String rangeHeader = requestHeaders.getFirst(HttpHeaders.RANGE);
        if (rangeHeader == null || rangeHeader.isBlank()) {
            return List.of();
        }
        return HttpRange.parseRanges(rangeHeader);
    }

    private static String contentTypeOrDefault(String contentType) {
        return contentType == null || contentType.isBlank() ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType;
    }

    private static Resource regionResource(Resource resource, long start, long count) {
        try {
            InputStream inputStream = resource.getInputStream();
            inputStream.skipNBytes(start);
            return new InputStreamResource(new BoundedInputStream(inputStream, count));
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored music file is missing", ex);
        }
    }

    public record StreamResource(MusicFile music, Resource resource, String profileId, String contentType,
            long contentLength) {
    }

    private static final class BoundedInputStream extends FilterInputStream {
        private long remaining;

        private BoundedInputStream(InputStream inputStream, long count) {
            super(inputStream);
            this.remaining = count;
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = super.read();
            if (value != -1) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int bytesRead = super.read(buffer, offset, (int) Math.min(length, remaining));
            if (bytesRead != -1) {
                remaining -= bytesRead;
            }
            return bytesRead;
        }
    }
}
