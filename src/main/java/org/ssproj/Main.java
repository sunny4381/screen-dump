package org.ssproj;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Main implements Runnable {
    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private final static AtomicBoolean done = new AtomicBoolean();
    private final static Set<URL> check = Collections.synchronizedSet(new HashSet<>());
    private final static AtomicLong totalCounter = new AtomicLong();

    public static void main(String[] args) throws IOException, ParseException {
        Options opts = new Options();
        opts.addOption("b", "browser", true, "specify one of browser: ie, firefox, chrome, edge. default is `ie`.");
        opts.addOption("o", "output", true, "specify output directory. default is `screenshots`.");
        opts.addOption("l", "limit", true, "specify screen shots limits. default is 0 (unlimited).");
        opts.addOption("i", "initial-sleep", true, "specify sleep in milliseconds before taking first screenshot. default is 5000.");
        opts.addOption("s", "sleep", true, "specify sleep in milliseconds before taking each screenshots. default is 500.");
        opts.addOption("c", "concurrency", true, "specify concurrency. default is 2.");
        opts.addOption("t", "traverse", true, "specify traverse other links. default is no traverse.");

        CommandLineParser parser = new DefaultParser();
        CommandLine cl = parser.parse(opts, args);

        final String browser;
        if (cl.hasOption('b')) {
            browser = cl.getOptionValue('b');
        } else {
            browser = "ie";
        }

        final String outputDirectory;
        if (cl.hasOption('o')) {
            outputDirectory = cl.getOptionValue('o');
        } else {
            outputDirectory = "screenshots";
        }

        final long screenShotLimit;
        if (cl.hasOption('l')) {
            screenShotLimit = Long.parseLong(cl.getOptionValue('l'));
        } else {
            screenShotLimit = 0;
        }

        final long initialSleep;
        if (cl.hasOption('i')) {
            initialSleep = Long.parseLong(cl.getOptionValue('i'));
        } else {
            initialSleep = 5000;
        }

        final long sleep;
        if (cl.hasOption('s')) {
            sleep = Long.parseLong(cl.getOptionValue('s'));
        } else {
            sleep = 500;
        }

        final int concurrency;
        if (cl.hasOption('c')) {
            concurrency = Integer.parseInt(cl.getOptionValue('c'));
        } else {
            concurrency = 2;
        }

        final TraverseSetting traverseSetting;
        if (cl.hasOption('t')) {
            traverseSetting = TraverseSetting.load(cl.getOptionValue('t'));
        } else {
            traverseSetting = null;
        }

        final BlockingQueue<URL> queue = new LinkedBlockingQueue<>();
        final Thread[] threads = new Thread[concurrency];
        for (int i = 0; i < concurrency; i++) {
            final Runnable runner = new Main(queue, outputDirectory, screenShotLimit, browser, initialSleep, sleep, traverseSetting);
            threads[i] = new Thread(runner);
            threads[i].start();
        }

        if (traverseSetting != null) {
            for (String url: traverseSetting.getSeeds()) {
                try {
                    queue.put(new URL(url));
                } catch (MalformedURLException e) {
                    LOGGER.error("malformed error", e);
                    continue;
                } catch (InterruptedException e) {
                    LOGGER.error("interrupted", e);
                    break;
                }
            }
        }

        for (String fileName: cl.getArgs()) {
            final LineIterator source;
            try {
                source = FileUtils.lineIterator(new File(fileName), "UTF-8");
            } catch (IOException e) {
                LOGGER.error("open error", e);
                return;
            }

            try {
                while (source.hasNext()) {
                    final URL url;
                    try {
                        url = new URL(source.next());
                    } catch (MalformedURLException e) {
                        LOGGER.error("malformed error", e);
                        continue;
                    }

                    queue.put(url);
                }
            } catch (InterruptedException e) {
                LOGGER.error("interrupted", e);
                break;
            } finally {
                source.close();
            }
        }

        while (queue.size() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOGGER.error("interrupted", e);
                break;
            }
        }

        done.set(true);
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                LOGGER.error("interrupted", e);
                break;
            }
        }
    }

    private final BlockingQueue<URL> queue;
    private final String outputDirectory;
    private final long screenShotLimit;
    private final WebDriver driver;
    private final Wait<WebDriver> wait;
    private final long initialSleep;
    private final long sleep;
    private final TraverseSetting traverseSetting;
    private final AtomicLong counter = new AtomicLong();

    public Main(BlockingQueue<URL> queue, String outputDirectory, long screenShotLimit, String browser, long initialSleep, long sleep, TraverseSetting traverseSetting) {
        this.queue = queue;
        this.outputDirectory = outputDirectory;
        this.screenShotLimit = screenShotLimit;
        if (browser.equalsIgnoreCase("firefox")) {
            LOGGER.info("activating firefox driver");
            this.driver = new FirefoxDriver();
        } else if (browser.equalsIgnoreCase("chrome")) {
            LOGGER.info("activating chrome driver");
            this.driver = new ChromeDriver();
        } else if (browser.equalsIgnoreCase("ie")) {
            LOGGER.info("activating internet explorer driver");
            this.driver = new InternetExplorerDriver();
        } else if (browser.equalsIgnoreCase("edge")) {
            LOGGER.info("activating edge driver");
            this.driver = new EdgeDriver();
        } else {
            throw new IllegalArgumentException("unknown browser");
        }
        this.wait = new WebDriverWait(this.driver, 30);
        this.initialSleep = initialSleep;
        this.sleep = sleep;
        this.traverseSetting = traverseSetting;
    }

    public void run() {
        LOGGER.debug("start taking screen shots");
        while (!done.get()) {
            final URL url;
            try {
                url = queue.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                break;
            }

            if (url == null) {
                continue;
            }

            if (check.add(url)) {
                saveScreenShot(url);
            }
        }

        this.driver.close();
        LOGGER.info("finish taking screen shots");
    }

    private void saveScreenShot(URL url) {
        if (exceedsScreenShotLimit(totalCounter.incrementAndGet())) {
            return;
        }
        if ((totalCounter.get() % 10) == 0) {
            reportStats();
        }

        LOGGER.info("visit: {}", url);
        this.driver.get(url.toString());

        final long sleep;
        if (counter.incrementAndGet() == 1) {
            sleep = this.initialSleep;
        } else {
            sleep = this.sleep;
        }

        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            return;
        }

        try {
            if (this.driver instanceof ChromeDriver) {
                saveChromeScreenShot(url);
            } else {
                File file = ((TakesScreenshot) this.driver).getScreenshotAs(OutputType.FILE);
                File outputFile = getOutputFile(url);
                FileUtils.copyFile(file, outputFile);
            }
        } catch (IOException e) {
            LOGGER.error("save error", e);
        }

        // traverse
        if (this.traverseSetting != null) {
            try {
                if (this.traverseSetting.allowsForExtraction(url)) {
                    traverse(url.toURI());
                }
            } catch (URISyntaxException e) {
                LOGGER.error("syntax error", e);
            }
        }
    }

    private void reportStats() {
        StringBuilder log = new StringBuilder();
        log.append(totalCounter.get());
        log.append(" screen shots taked");

        log.append("; ");

        log.append(queue.size());
        log.append(" urls remaining");

        LOGGER.info(log.toString());
    }

    private boolean exceedsScreenShotLimit(long count) {
        if (this.screenShotLimit <= 0) {
            // unlimited
            return false;
        }

        return this.screenShotLimit < count;
    }

    private void saveChromeScreenShot(URL url) throws IOException {
        JavascriptExecutor jexec = (JavascriptExecutor) driver;

        //画面サイズで必要なものを取得
        int innerH = Integer.parseInt(String.valueOf(jexec.executeScript("return window.innerHeight")));
        int innerW =Integer.parseInt(String.valueOf(jexec.executeScript("return window.innerWidth")));
        int scrollH = Integer.parseInt(String.valueOf(jexec.executeScript("return document.documentElement.scrollHeight")));

        //イメージを扱うための準備
        BufferedImage img = new BufferedImage(innerW, scrollH, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();

        //スクロールを行うかの判定
        if (innerH > scrollH) {
            BufferedImage imageParts = ImageIO.read(((TakesScreenshot) this.driver).getScreenshotAs(OutputType.FILE));
            g.drawImage(imageParts, 0, 0, null);
        } else {
            int scrollableH = scrollH;
            int i = 0;

            //スクロールしながらなんどもイメージを結合していく
            while (scrollableH > innerH) {
                BufferedImage imageParts = ImageIO.read(((TakesScreenshot) this.driver).getScreenshotAs(OutputType.FILE));
                g.drawImage(imageParts, 0, innerH * i, null);
                scrollableH = scrollableH - innerH;
                i++;
                jexec.executeScript("window.scrollTo(0," + innerH * i + ")");

                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }

            //一番下まで行ったときは、下から埋めるように貼り付け
            BufferedImage imageParts = ImageIO.read(((TakesScreenshot) this.driver).getScreenshotAs(OutputType.FILE));
            g.drawImage(imageParts, 0, scrollH - innerH, null);
        }

        ImageIO.write(img, "png", getOutputFile(url));
    }

    private File getOutputFile(URL url) {
        String outputFileName = url.getPath();
        if (outputFileName.endsWith("/")) {
            outputFileName = outputFileName + "index.html";
        }
        outputFileName = outputFileName + ".png";

        File outputFile = new File(new File(this.outputDirectory), outputFileName);
        File outputDirectory = outputFile.getParentFile();
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs();
        }

        return outputFile;
    }

    private void traverse(final URI baseUri) {
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
                if (check.contains(newUrl)) {
                    continue;
                }
                if (!this.traverseSetting.allowsForAccess(newUrl)) {
                    continue;
                }

                this.queue.put(newUrl);
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
}
