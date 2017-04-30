package org.ssproj;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public abstract class Traverser implements Closeable {
    public final static Logger LOGGER = LoggerFactory.getLogger(Traverser.class);
    private final static Marker LINK = MarkerFactory.getMarker("link");
    private TraverserContext context;
    private WebDriver driver;
    private final AtomicLong counter = new AtomicLong();

    public Traverser(TraverserContext context) {
        this.context = context;
    }

    public WebDriver getDriver() {
        return this.driver;
    }

    public TraverserContext getContext() {
        return this.context;
    }

    protected abstract WebDriver startDriver();

    protected void initDriver() {
        if (this.driver != null) {
            return;
        }

        this.driver = startDriver();

        WebDriver.Window window = this.driver.manage().window();
        org.openqa.selenium.Dimension size = window.getSize();

        int width = getContext().getWidth();
        if (width <= 0) {
            width = size.getWidth();
        }
        int height = getContext().getHeight();
        if (height <= 0) {
            height = size.getHeight();
        }

        if (size.getWidth() != width || size.getHeight() != height) {
            window.setSize(new org.openqa.selenium.Dimension(width, height));
        }
    }

    public void visit(URL url) throws InterruptedException {
        if (this.driver == null) {
            initDriver();
        }

        if (!getContext().addCheck(url)) {
            LOGGER.debug("{}: already visited", url);
            return;
        }

        long totalCount = getContext().incrementTotalCounter();
        if (exceedsScreenShotLimit(totalCount)) {
            return;
        }
        if ((totalCount % 10) == 0) {
            reportStats();
        }

        LOGGER.info("visit: {}", url);
        try {
            this.driver.get(url.toString());
        } catch (org.openqa.selenium.WebDriverException e) {
            final String failureMessage = String.format("error occurs while visiting %s", url);
            LOGGER.warn(failureMessage, e);
            return;
        }

        final long sleep;
        if (this.counter.incrementAndGet() == 1) {
            sleep = this.context.getInitialSleep();
        } else {
            sleep = this.context.getSleep();
        }

        Thread.sleep(sleep);

        try {
            saveScreenShot();
        } catch (IOException e) {
            LOGGER.error("save error", e);
        }

        // traverse
        if (canTraverse()) {
            traverse();
        }
    }

    private boolean canTraverse() {
        if (!getContext().traversesRecursively()) {
            return false;
        }

        final URL url;
        try {
            url = new URL(this.driver.getCurrentUrl());
        } catch (MalformedURLException e) {
            LOGGER.warn(this.driver.getCurrentUrl() + ": malformed url", e);
            return false;
        }

        return this.getContext().allowsForExtraction(url);
    }

    private void reportStats() {
        StringBuilder log = new StringBuilder();
        log.append(this.context.getTotalCounter().get());
        log.append(" screen shots taked");

        log.append("; ");

        log.append(this.context.getQueue().size());
        log.append(" urls remaining");

        LOGGER.info(log.toString());
    }

    private boolean exceedsScreenShotLimit(long count) {
        if (this.context.getScreenShotLimit() <= 0) {
            // unlimited
            return false;
        }

        return this.context.getScreenShotLimit() < count;
    }

    protected void saveScreenShot() throws IOException, InterruptedException {
        File file = ((TakesScreenshot) this.driver).getScreenshotAs(OutputType.FILE);
        File outputFile = getOutputFile();
        FileUtils.copyFile(file, outputFile);
        compressPng(outputFile);
        LOGGER.debug("saved screen shot: {}", outputFile);
    }

    protected File getOutputFile() {
        final URL url;
        try {
            url = new URL(this.driver.getCurrentUrl());
        } catch (MalformedURLException e) {
            LOGGER.warn(this.driver.getCurrentUrl() + ": malformed url", e);
            return null;
        }

        String host = url.getHost();
        int port = url.getPort();
        String hostDirectory = port == -1 ? host : host + "_" + port;

        String outputFileName = url.getPath();
        if (outputFileName.endsWith("/")) {
            outputFileName = outputFileName + "index.html";
        }
        outputFileName = outputFileName + ".png";

        Pattern regex = Pattern.compile("/\\.");
        outputFileName = regex.matcher(outputFileName).replaceAll("/_");

        File outputFile = new File(new File(this.context.getOutputDirectory(), hostDirectory), outputFileName);
        File outputDirectory = outputFile.getParentFile();
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        return outputFile;
    }

    protected void compressPng(File file) throws InterruptedException {
        if (! file.exists()) {
            return;
        }

        final String command = this.getContext().getPngQuant();
        if (command == null || command.isEmpty()) {
            return;
        }

        final PNGCompressor compressor = new PNGCompressor(command);
        compressor.apply(file);
    }

    private void traverse() throws InterruptedException {
        final AnchorExtractor extractor = new AnchorExtractor(this.driver);
        final Href2URI href2uri;
        try {
            href2uri = new Href2URI(this.driver);
        } catch (URISyntaxException e) {
            LOGGER.error("uri syntax exception", e);
            return;
        }

        Set<String> dups = new HashSet<>();
        for (URL url: href2uri.apply(extractor.apply())) {
            if (!dups.add(url.toString())) {
                continue;
            }

            LOGGER.debug(LINK, "{}\t{}", href2uri.getBaseURI(), url);
            if (getContext().containsCheck(url)) {
                LOGGER.debug("{}: already visited", url);
                continue;
            }
            if (!getContext().allowsForAccess(url)) {
                LOGGER.debug("{}: not allowed for access", url);
                continue;
            }

            this.getContext().getQueue().put(url);
        }
    }

    @Override
    public void close() {
        if (this.driver != null) {
            this.driver.close();
            this.driver = null;
        }
    }
}
