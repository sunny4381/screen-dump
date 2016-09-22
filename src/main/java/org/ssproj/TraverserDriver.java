package org.ssproj;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TraverserDriver {
    private final static Logger LOGGER = LoggerFactory.getLogger(TraverserDriver.class);
    private final TraverserContext context;
    private final List<String> fileNames;
    private Thread[] threads;

    public TraverserDriver(TraverserContext context, List<String> fileNames) {
        this.context = context;
        this.fileNames = fileNames;
    }

    public static void run(TraverserContext context, List<String> fileNames) {
        TraverserDriver driver = new TraverserDriver(context, fileNames);
        try {
            driver.start();
            driver.join();
        } catch (InterruptedException e) {
            LOGGER.error("interrupted", e);
        }
    }

    public TraverserContext getContext() {
        return this.context;
    }

    public void start() throws InterruptedException {
        this.threads = new Thread[getContext().getConcurrency()];
        for (int i = 0; i < getContext().getConcurrency(); i++) {
            this.threads[i] = new Thread(() -> run());
            this.threads[i].start();
        }

        feedUrls(getContext().getSeeds());

        for (String fileName: fileNames) {
            feedUrlsFromFile(fileName);
        }
    }

    private void feedUrls(Iterable<String> urls) throws InterruptedException {
        feedUrls(urls.iterator());
    }

    private void feedUrls(Iterator<String> urls) throws InterruptedException {
        while (urls.hasNext()) {
            final String url = urls.next();
            try {
                getContext().getQueue().put(new URL(url));
            } catch (MalformedURLException e) {
                LOGGER.error(url + ": malformed error", e);
                continue;
            }
        }
    }

    private void feedUrlsFromFile(String fileName) throws InterruptedException {
        feedUrlsFromFile(new File(fileName));
    }

    private void feedUrlsFromFile(File file) throws InterruptedException {
        final LineIterator source;
        try {
            source = FileUtils.lineIterator(file, "UTF-8");
        } catch (IOException e) {
            LOGGER.error(file.getAbsolutePath() + ": file open error", e);
            return;
        }

        try {
            feedUrls(source);
        } finally {
            source.close();
        }
    }

    public void join() throws InterruptedException {
        while (getContext().getQueue().size() > 0) {
            Thread.sleep(100);
        }

        getContext().setDone(true);
        for (Thread thread : this.threads) {
            thread.join();
        }
    }

    private void run() {
        LOGGER.debug("start taking screen shots");

        final Traverser traverser = TraverserFactory.create(getContext());
        while (!getContext().getDone()) {
            try {
                final URL url = getContext().getQueue().poll(500, TimeUnit.MILLISECONDS);
                if (url == null) {
                    continue;
                }

                traverser.visit(url);
            } catch (RuntimeException e) {
                LOGGER.info("unexpected exception", e);
            } catch (InterruptedException e) {
                LOGGER.info("interrupted");
                break;
            }
        }

        traverser.close();
        LOGGER.info("finish taking screen shots");
    }
}
