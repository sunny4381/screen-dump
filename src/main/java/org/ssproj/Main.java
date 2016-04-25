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
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

public class Main {
    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, ParseException {
        Options opts = new Options();
        opts.addOption("b", "browser", true, "specify one of browser: ie, firefox, chrome, edge. default is `ie`.");
        opts.addOption("o", "output", true, "specify output directory. default is `screenshots`.");
        opts.addOption("i", "initial-sleep", true, "specify sleep in milliseconds before taking first screenshot. default is 5000.");
        opts.addOption("s", "sleep", true, "specify sleep in milliseconds before taking each screenshots. default is 500.");

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

        args = cl.getArgs();
        final Main main = new Main(args[0], outputDirectory, browser, initialSleep, sleep);
        main.run();
    }

    private final String listFileName;
    private final String outputDirectory;
    private final WebDriver driver;
    private final Wait<WebDriver> wait;
    private final long initialSleep;
    private final long sleep;
    private final AtomicLong counter = new AtomicLong();

    public Main(String listFileName, String outputDirectory, String browser, long initialSleep, long sleep) {
        this.listFileName = listFileName;
        this.outputDirectory = outputDirectory;
        if (browser.equalsIgnoreCase("firefox")) {
            System.out.println("activating firefox driver");
            this.driver = new FirefoxDriver();
        } else if (browser.equalsIgnoreCase("chrome")) {
            System.out.println("activating chrome driver");
            this.driver = new ChromeDriver();
        } else if (browser.equalsIgnoreCase("ie")) {
            System.out.println("activating internet explorer driver");
            this.driver = new InternetExplorerDriver();
        } else if (browser.equalsIgnoreCase("edge")) {
            System.out.println("activating edge driver");
            this.driver = new EdgeDriver();
        } else {
            throw new IllegalArgumentException("unknown browser");
        }
        this.wait = new WebDriverWait(this.driver, 30);
        this.initialSleep = initialSleep;
        this.sleep = sleep;
    }

    public void run() {
        final LineIterator source;
        try {
            source = FileUtils.lineIterator(new File(listFileName), "UTF-8");
        } catch (IOException e) {
            LOGGER.error("open error", e);
            return;
        }

        while (source.hasNext()) {
            final URL url;
            try {
                url = new URL(source.next());
            } catch (MalformedURLException e) {
                LOGGER.error("malformed error", e);
                continue;
            }

            saveScreenShot(url);
        }

        this.driver.close();
    }

    private void saveScreenShot(URL url) {
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
}
