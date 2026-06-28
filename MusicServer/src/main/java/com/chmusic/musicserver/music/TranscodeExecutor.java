package com.chmusic.musicserver.music;

import com.chmusic.musicserver.config.MusicServerProperties;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TranscodeExecutor {
    private static final Duration PROCESS_TIMEOUT = Duration.ofMinutes(30);

    private final String ffmpegPath;
    private final ExecutorService workerPool;

    public TranscodeExecutor(MusicServerProperties properties) {
        MusicServerProperties.Transcoding transcoding = properties.getTranscoding();
        this.ffmpegPath = transcoding.getFfmpegPath();
        int maxConcurrency = Math.max(1, transcoding.getMaxConcurrency());
        this.workerPool = Executors.newFixedThreadPool(maxConcurrency, runnable -> {
            Thread thread = new Thread(runnable, "music-transcode-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<TranscodeResult> submit(Path sourcePath,
            TranscodeProfileCatalog.ProfileDescriptor profile, Path tempOutputPath) {
        return CompletableFuture.supplyAsync(() -> execute(sourcePath, profile, tempOutputPath), workerPool);
    }

    public TranscodeResult execute(Path sourcePath, TranscodeProfileCatalog.ProfileDescriptor profile,
            Path tempOutputPath) {
        if (!StringUtils.hasText(ffmpegPath)) {
            return TranscodeResult.failed("FFMPEG_PATH_EMPTY");
        }
        try {
            Files.createDirectories(tempOutputPath.getParent());
            Files.deleteIfExists(tempOutputPath);
            List<String> command = command(sourcePath, profile, tempOutputPath);
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start();
            boolean finished = process.waitFor(PROCESS_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return TranscodeResult.failed("TRANSCODE_TIMEOUT");
            }
            if (process.exitValue() != 0) {
                return TranscodeResult.failed("TRANSCODE_EXIT_" + process.exitValue());
            }
            if (!Files.isRegularFile(tempOutputPath)) {
                return TranscodeResult.failed("TRANSCODE_OUTPUT_MISSING");
            }
            return TranscodeResult.ready(tempOutputPath);
        } catch (IllegalArgumentException ex) {
            return TranscodeResult.failed("TRANSCODE_ARGS_INVALID");
        } catch (IOException ex) {
            return TranscodeResult.failed("TRANSCODE_IO_ERROR");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return TranscodeResult.failed("TRANSCODE_INTERRUPTED");
        }
    }

    public boolean canExecute() {
        return StringUtils.hasText(ffmpegPath) && !workerPool.isShutdown();
    }

    @PreDestroy
    void shutdown() {
        workerPool.shutdownNow();
    }

    private List<String> command(Path sourcePath, TranscodeProfileCatalog.ProfileDescriptor profile,
            Path tempOutputPath) {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");
        command.add("-i");
        command.add(sourcePath.toString());
        command.addAll(parseArgs(profile.args()));
        command.add(tempOutputPath.toString());
        return command;
    }

    private static List<String> parseArgs(String args) {
        if (!StringUtils.hasText(args)) {
            return List.of();
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        for (int i = 0; i < args.length(); i++) {
            char ch = args.charAt(i);
            if ((ch == '"' || ch == '\'') && quote == 0) {
                quote = ch;
                continue;
            }
            if (ch == quote) {
                quote = 0;
                continue;
            }
            if (Character.isWhitespace(ch) && quote == 0) {
                addToken(tokens, current);
                continue;
            }
            current.append(ch);
        }
        if (quote != 0) {
            throw new IllegalArgumentException("Unclosed quote in transcode args");
        }
        addToken(tokens, current);
        return tokens;
    }

    private static void addToken(List<String> tokens, StringBuilder current) {
        if (!current.isEmpty()) {
            tokens.add(current.toString());
            current.setLength(0);
        }
    }

    public record TranscodeResult(boolean ready, Path outputPath, String errorCode) {
        public static TranscodeResult ready(Path outputPath) {
            return new TranscodeResult(true, outputPath, null);
        }

        public static TranscodeResult failed(String errorCode) {
            return new TranscodeResult(false, null, errorCode);
        }
    }
}
