package org.ssproj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by nakano_hideo on 2016/09/19.
 */
public class PNGCompressor {
    private final static Logger LOGGER = LoggerFactory.getLogger(PNGCompressor.class);
    private final String command;

    public PNGCompressor(String command) {
        this.command = command;
    }

    public boolean apply(final File file) {
        final Process p = startProcess(file);
        if (p == null) {
            return false;
        }

        try {
            comsumeInputStream(p);
        } catch (IOException e) {
            String failureMessage = "some errors occurred while executing command: " + this.command;
            LOGGER.warn(failureMessage, e);
            return false;
        }

        int exitValue = p.exitValue();
        if (exitValue != 0) {
            LOGGER.warn("{} is existed non-success value: {} ", command, exitValue);
            return false;
        }

        return true;
    }

    @org.jetbrains.annotations.Nullable
    private Process startProcess(File file) {
        final ProcessBuilder pb = new ProcessBuilder(this.command, "--ext", ".png", "--force", file.getAbsolutePath());
        pb.redirectErrorStream(true);
        try {
            return pb.start();
        } catch (IOException e) {
            String failureMessage = "failed to execute command: " + this.command;
            LOGGER.warn(failureMessage, e);
        }
        return null;
    }

    private void comsumeInputStream(Process p) throws IOException {
        comsumeInputStream(p.getInputStream());
    }

    private void comsumeInputStream(InputStream is) throws IOException {
        try {
            while (is.read() >= 0);
        } finally {
            is.close();
        }
    }
}
