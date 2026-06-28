package com.chmusic.musicserver.music;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TranscodeToolProbe {
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(3);

    public ProbeResult probe(String executablePath) {
        if (!StringUtils.hasText(executablePath)) {
            return ProbeResult.unavailable("TOOL_PATH_EMPTY");
        }

        Process process = null;
        try {
            process = new ProcessBuilder(executablePath, "-version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = process.waitFor(PROBE_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroyForcibly();
                return ProbeResult.unavailable("TOOL_PROBE_TIMEOUT");
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                return ProbeResult.success();
            }
            return ProbeResult.unavailable("TOOL_EXIT_" + exitCode);
        } catch (IOException ex) {
            return ProbeResult.unavailable("TOOL_NOT_EXECUTABLE");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ProbeResult.unavailable("TOOL_PROBE_INTERRUPTED");
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    public record ProbeResult(boolean available, String reason) {
        public static ProbeResult success() {
            return new ProbeResult(true, "AVAILABLE");
        }

        public static ProbeResult unavailable(String reason) {
            return new ProbeResult(false, reason);
        }
    }
}
