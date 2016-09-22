package org.ssproj;

import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class TraverserContext {
    private final TraverseSetting setting;
    private final BlockingQueue<URL> queue = new LinkedBlockingQueue<>();
    private final AtomicBoolean done = new AtomicBoolean();
    private final Set<URL> check = Collections.synchronizedSet(new HashSet<>());
    private final AtomicLong totalCounter = new AtomicLong();

    public TraverserContext() {
        this.setting = new TraverseSetting();
    }

    private TraverserContext(TraverseSetting setting) {
        this.setting = setting;
    }

    public static TraverserContext load(String fileName) throws IOException {
        return new TraverserContext(TraverseSetting.load(fileName));
    }

    public static TraverserContext load(Reader input) throws IOException {
        return new TraverserContext(TraverseSetting.load(input));
    }

    public TraverseSetting getSetting() {
        return this.setting;
    }
    public String getBrowser() {
        return getSetting().getBrowser();
    }

    public void setBrowser(String browser) {
        getSetting().setBrowser(browser);
    }

    public String getOutputDirectory() {
        return getSetting().getOutputDirectory();
    }

    public void setOutputDirectory(String outputDirectory) {
        getSetting().setOutputDirectory(outputDirectory);
    }

    public long getScreenShotLimit() {
        return getSetting().getScreenShotLimit();
    }

    public void setScreenShotLimit(long screenShotLimit) {
        getSetting().setScreenShotLimit(screenShotLimit);
    }

    public long getInitialSleep() {
        return getSetting().getInitialSleep();
    }

    public void setInitialSleep(long initialSleep) {
        getSetting().setInitialSleep(initialSleep);
    }

    public long getSleep() {
        return getSetting().getSleep();
    }

    public void setSleep(long sleep) {
        getSetting().setSleep(sleep);
    }

    public int getConcurrency() {
        return getSetting().getConcurrency();
    }

    public void setConcurrency(int concurrency) {
        getSetting().setConcurrency(concurrency);
    }

    public int getWidth() {
        return getSetting().getWidth();
    }

    public void setWidth(int width) {
        getSetting().setWidth(width);
    }

    public int getHeight() {
        return getSetting().getHeight();
    }

    public void setHeight(int height) {
        getSetting().setHeight(height);
    }

    public List<String> getSeeds() {
        return getSetting().getSeeds();
    }

    public boolean getRecursively() {
        return getSetting().getRecursively();
    }

    public void setRecursively(boolean recursively) {
        getSetting().setRecursively(recursively);
    }

    public boolean traversesRecursively() {
        return getRecursively();
    }

    public BlockingQueue<URL> getQueue() {
        return this.queue;
    }

    public AtomicLong getTotalCounter() {
        return totalCounter;
    }

//    public Set<URL> getCheck() {
//        return check;
//    }

    public boolean addCheck(URL url) {
        return this.check.add(url);
    }

    public boolean getDone() {
        return done.get();
    }

    public void setDone(boolean done) {
        this.done.set(done);
    }

    public boolean allowsForExtraction(URL url) {
        return getSetting().allowsForExtraction(url);
    }

    public String getPngQuant() {
        return getSetting().getPngQuant();
    }

    public boolean containsCheck(URL url) {
        return this.check.contains(url);
    }

    public boolean allowsForAccess(URL url) {
        return getSetting().allowsForAccess(url);
    }

    public long incrementTotalCounter() {
        return this.totalCounter.incrementAndGet();
    }
}
