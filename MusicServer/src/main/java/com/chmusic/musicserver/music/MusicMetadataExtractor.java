package com.chmusic.musicserver.music;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MusicMetadataExtractor {
    private static final Logger log = LoggerFactory.getLogger(MusicMetadataExtractor.class);
    private static final int MAX_TAG_SIZE = 16 * 1024 * 1024;
    private static final int MAX_COVER_SIZE = 5 * 1024 * 1024;
    private static final int MPEG_SCAN_LIMIT = 256 * 1024;
    private static final int[] MPEG1_LAYER3_BITRATES = {
            0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 0
    };
    private static final int[] MPEG2_LAYER3_BITRATES = {
            0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160, 0
    };

    public MusicMetadata extract(Path path) {
        try {
            Id3v2Metadata id3v2 = readId3v2(path);
            MusicMetadata id3v1 = id3v2.hasTextMetadata() ? MusicMetadata.empty() : readId3v1(path);
            Long duration = firstNonNull(id3v2.duration(), estimateMp3Duration(path, id3v2.audioStart()));
            return new MusicMetadata(
                    firstNonBlank(id3v2.title(), id3v1.title()),
                    firstNonBlank(id3v2.artist(), id3v1.artist()),
                    firstNonBlank(id3v2.album(), id3v1.album()),
                    duration,
                    id3v2.cover());
        } catch (IOException | RuntimeException ex) {
            log.debug("Failed to extract music metadata from {}", path, ex);
            return MusicMetadata.empty();
        }
    }

    private static Id3v2Metadata readId3v2(Path path) throws IOException {
        if (Files.size(path) < 10) {
            return Id3v2Metadata.empty(0);
        }
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            byte[] header = new byte[10];
            file.readFully(header);
            if (header[0] != 'I' || header[1] != 'D' || header[2] != '3') {
                return Id3v2Metadata.empty(0);
            }

            int version = Byte.toUnsignedInt(header[3]);
            int tagSize = synchsafeInt(header, 6);
            long audioStart = 10L + tagSize;
            if (tagSize <= 0 || tagSize > MAX_TAG_SIZE) {
                return Id3v2Metadata.empty(audioStart);
            }

            byte[] tag = new byte[tagSize];
            file.readFully(tag);
            return parseId3v2Frames(tag, version, audioStart);
        }
    }

    private static Id3v2Metadata parseId3v2Frames(byte[] tag, int version, long audioStart) {
        String title = null;
        String artist = null;
        String album = null;
        Long duration = null;
        EmbeddedCover cover = null;

        int offset = 0;
        while (offset + (version == 2 ? 6 : 10) <= tag.length) {
            Frame frame = version == 2 ? readV22Frame(tag, offset) : readV23OrV24Frame(tag, offset, version);
            if (frame == null || frame.size() <= 0 || frame.dataOffset() + frame.size() > tag.length) {
                break;
            }

            byte[] payload = Arrays.copyOfRange(tag, frame.dataOffset(), frame.dataOffset() + frame.size());
            String id = frame.id();
            if ("TIT2".equals(id) || "TT2".equals(id)) {
                title = readTextFrame(payload);
            } else if ("TPE1".equals(id) || "TP1".equals(id)) {
                artist = readTextFrame(payload);
            } else if ("TALB".equals(id) || "TAL".equals(id)) {
                album = readTextFrame(payload);
            } else if ("TLEN".equals(id) || "TLE".equals(id)) {
                duration = parseDuration(readTextFrame(payload));
            } else if (cover == null && ("APIC".equals(id) || "PIC".equals(id))) {
                cover = "PIC".equals(id) ? readV22CoverFrame(payload) : readCoverFrame(payload);
            }

            offset = frame.dataOffset() + frame.size();
        }

        return new Id3v2Metadata(clean(title), clean(artist), clean(album), duration, cover, audioStart);
    }

    private static Frame readV22Frame(byte[] tag, int offset) {
        String id = new String(tag, offset, 3, StandardCharsets.ISO_8859_1);
        if (id.trim().isEmpty() || tag[offset] == 0) {
            return null;
        }
        int size = ((Byte.toUnsignedInt(tag[offset + 3]) & 0xff) << 16)
                | ((Byte.toUnsignedInt(tag[offset + 4]) & 0xff) << 8)
                | (Byte.toUnsignedInt(tag[offset + 5]) & 0xff);
        return new Frame(id, size, offset + 6);
    }

    private static Frame readV23OrV24Frame(byte[] tag, int offset, int version) {
        String id = new String(tag, offset, 4, StandardCharsets.ISO_8859_1);
        if (id.trim().isEmpty() || tag[offset] == 0) {
            return null;
        }
        int size = version == 4 ? synchsafeInt(tag, offset + 4) : ByteBuffer.wrap(tag, offset + 4, 4).getInt();
        return new Frame(id, size, offset + 10);
    }

    private static String readTextFrame(byte[] payload) {
        if (payload.length <= 1) {
            return null;
        }
        Charset charset = charsetForEncoding(payload[0]);
        return stripNulls(new String(payload, 1, payload.length - 1, charset));
    }

    private static EmbeddedCover readCoverFrame(byte[] payload) {
        if (payload.length < 4) {
            return null;
        }
        int offset = 1;
        int mimeEnd = indexOfZero(payload, offset);
        if (mimeEnd < 0 || mimeEnd + 2 >= payload.length) {
            return null;
        }
        String contentType = new String(payload, offset, mimeEnd - offset, StandardCharsets.ISO_8859_1);
        offset = mimeEnd + 2;
        int descriptionEnd = findEncodedTerminator(payload, offset, payload[0]);
        if (descriptionEnd < 0) {
            return null;
        }
        offset = descriptionEnd + terminatorLength(payload[0]);
        return coverFromPayload(contentType, payload, offset);
    }

    private static EmbeddedCover readV22CoverFrame(byte[] payload) {
        if (payload.length < 6) {
            return null;
        }
        String format = new String(payload, 1, 3, StandardCharsets.ISO_8859_1).toLowerCase();
        String contentType = switch (format) {
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            default -> "image/jpeg";
        };
        int offset = 5;
        int descriptionEnd = findEncodedTerminator(payload, offset, payload[0]);
        if (descriptionEnd < 0) {
            return null;
        }
        offset = descriptionEnd + terminatorLength(payload[0]);
        return coverFromPayload(contentType, payload, offset);
    }

    private static EmbeddedCover coverFromPayload(String contentType, byte[] payload, int offset) {
        if (offset >= payload.length || payload.length - offset > MAX_COVER_SIZE) {
            return null;
        }
        String normalizedType = contentType == null || contentType.isBlank() ? "image/jpeg" : contentType.trim();
        return new EmbeddedCover(normalizedType, Arrays.copyOfRange(payload, offset, payload.length));
    }

    private static MusicMetadata readId3v1(Path path) throws IOException {
        if (Files.size(path) < 128) {
            return MusicMetadata.empty();
        }
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            file.seek(file.length() - 128);
            byte[] tag = new byte[128];
            file.readFully(tag);
            if (tag[0] != 'T' || tag[1] != 'A' || tag[2] != 'G') {
                return MusicMetadata.empty();
            }
            return new MusicMetadata(
                    clean(new String(tag, 3, 30, StandardCharsets.ISO_8859_1)),
                    clean(new String(tag, 33, 30, StandardCharsets.ISO_8859_1)),
                    clean(new String(tag, 63, 30, StandardCharsets.ISO_8859_1)),
                    null,
                    null);
        }
    }

    private static Long estimateMp3Duration(Path path, long audioStart) throws IOException {
        long fileSize = Files.size(path);
        if (audioStart >= fileSize) {
            return null;
        }
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            file.seek(audioStart);
            int scanned = 0;
            while (scanned < MPEG_SCAN_LIMIT && file.getFilePointer() + 4 < fileSize) {
                long position = file.getFilePointer();
                int header = file.readInt();
                scanned += 1;
                Integer bitrateKbps = parseMpegBitrateKbps(header);
                if (bitrateKbps != null && bitrateKbps > 0) {
                    return ((fileSize - position) * 8L) / bitrateKbps;
                }
                file.seek(position + 1);
            }
        }
        return null;
    }

    private static Integer parseMpegBitrateKbps(int header) {
        if ((header & 0xffe00000) != 0xffe00000) {
            return null;
        }
        int versionBits = (header >> 19) & 0x3;
        int layerBits = (header >> 17) & 0x3;
        int bitrateIndex = (header >> 12) & 0xf;
        int sampleRateIndex = (header >> 10) & 0x3;
        if (versionBits == 1 || layerBits != 1 || bitrateIndex == 0 || bitrateIndex == 15
                || sampleRateIndex == 3) {
            return null;
        }
        return versionBits == 3 ? MPEG1_LAYER3_BITRATES[bitrateIndex] : MPEG2_LAYER3_BITRATES[bitrateIndex];
    }

    private static Long parseDuration(String value) {
        String cleaned = clean(value);
        if (cleaned == null) {
            return null;
        }
        try {
            long duration = Long.parseLong(cleaned.replaceAll("[^0-9]", ""));
            return duration > 0 ? duration : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Charset charsetForEncoding(byte encoding) {
        return switch (Byte.toUnsignedInt(encoding)) {
            case 1 -> StandardCharsets.UTF_16;
            case 2 -> StandardCharsets.UTF_16BE;
            case 3 -> StandardCharsets.UTF_8;
            default -> StandardCharsets.ISO_8859_1;
        };
    }

    private static int synchsafeInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0x7f) << 21)
                | ((bytes[offset + 1] & 0x7f) << 14)
                | ((bytes[offset + 2] & 0x7f) << 7)
                | (bytes[offset + 3] & 0x7f);
    }

    private static int indexOfZero(byte[] bytes, int offset) {
        for (int i = offset; i < bytes.length; i += 1) {
            if (bytes[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int findEncodedTerminator(byte[] bytes, int offset, byte encoding) {
        int length = terminatorLength(encoding);
        for (int i = offset; i <= bytes.length - length; i += length == 2 ? 2 : 1) {
            if (length == 1 && bytes[i] == 0) {
                return i;
            }
            if (length == 2 && bytes[i] == 0 && bytes[i + 1] == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int terminatorLength(byte encoding) {
        int value = Byte.toUnsignedInt(encoding);
        return value == 1 || value == 2 ? 2 : 1;
    }

    private static String stripNulls(String value) {
        int firstNull = value.indexOf('\0');
        return firstNull >= 0 ? value.substring(0, firstNull) : value;
    }

    private static String clean(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = value.replace('\u0000', ' ').trim();
        return cleaned.isBlank() ? null : cleaned;
    }

    private static String firstNonBlank(String first, String second) {
        String cleanedFirst = clean(first);
        return cleanedFirst != null ? cleanedFirst : clean(second);
    }

    private static <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private record Frame(String id, int size, int dataOffset) {
    }

    private record Id3v2Metadata(String title, String artist, String album, Long duration, EmbeddedCover cover,
            long audioStart) {
        static Id3v2Metadata empty(long audioStart) {
            return new Id3v2Metadata(null, null, null, null, null, audioStart);
        }

        boolean hasTextMetadata() {
            return title != null || artist != null || album != null;
        }
    }
}
