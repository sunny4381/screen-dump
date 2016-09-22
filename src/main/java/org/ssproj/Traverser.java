package org.ssproj;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

public abstract class Traverser implements Closeable {
    public final static Logger LOGGER = LoggerFactory.getLogger(Traverser.class);
    private TraverserContext context;
    private WebDriver driver;
    private Wait<WebDriver> wait;
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
    }

    protected File getOutputFile() {
        final URL url;
        try {
            url = new URL(this.driver.getCurrentUrl());
        } catch (MalformedURLException e) {
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

    private void traverse() {
        final URI baseUri;
        try {
            baseUri = new URI(this.driver.getCurrentUrl());
        } catch (URISyntaxException e) {
            return;
        }

        for (WebElement element : this.driver.findElements(By.tagName("a"))) {
            try {
                final String href = element.getAttribute("href");
                if (href == null || href.isEmpty()) {
                    continue;
                }

                URI uri = baseUri.resolve(href).normalize();
                if (! baseUri.getHost().equalsIgnoreCase(uri.getHost())) {
                    continue;
                }

                uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), null);
                URL newUrl = uri.toURL();
                if (getContext().containsCheck(newUrl)) {
                    continue;
                }
                if (!getContext().allowsForAccess(newUrl)) {
                    continue;
                }

                this.context.getQueue().put(newUrl);
            } catch (RuntimeException e) {
                LOGGER.error("some error", e);
            } catch (MalformedURLException | URISyntaxException e) {
                LOGGER.error("malformed error", e);
            } catch (InterruptedException e) {
                LOGGER.error("interrupted", e);
                break;
            }
        }
    }

    public void close() {
        if (this.driver != null) {
            this.driver.close();
            this.driver = null;
        }
    }
}
