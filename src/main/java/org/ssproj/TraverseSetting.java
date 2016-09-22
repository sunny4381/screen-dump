package org.ssproj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.Collections;
import java.util.List;

public class TraverseSetting {
    public static final boolean DEFAULT_RECURSIVELY = false;
    public static final String DEFAULT_BROWSER = "firefox";
    public static final int DEFAULT_CONCURRENCY = 2;
    public static final String DEFAULT_OUTPUT_DIRECTORY = "screenshots";
    public static final long DEFAULT_SCREEN_SHOT_LIMIT = 0;
    public static final long DEFAULT_INITIAL_SLEEP = 5000;
    public static final long DEFAULT_SLEEP = 500;
    public static final int DEFAULT_WIDTH = 0;
    public static final int DEFAULT_HEIGHT = 0;
    private final static Logger LOGGER = LoggerFactory.getLogger(TraverseSetting.class);

    private boolean recursively = DEFAULT_RECURSIVELY;
    private String browser = DEFAULT_BROWSER;
    private int concurrency = DEFAULT_CONCURRENCY;
    private String outputDirectory = DEFAULT_OUTPUT_DIRECTORY;
    private long screenShotLimit = DEFAULT_SCREEN_SHOT_LIMIT;
    private long initialSleep = DEFAULT_INITIAL_SLEEP;
    private long sleep = DEFAULT_SLEEP;
    private int width = DEFAULT_WIDTH;
    private int height = DEFAULT_HEIGHT;
    private List<String> seeds = Collections.emptyList();
    private List<String> accessibleDomains = Collections.emptyList();
    private List<String> extractableDomains = Collections.emptyList();
    private List<String> excludePaths = Collections.emptyList();
    private List<String> allowSuffixes = Collections.emptyList();
    private String pngQuant;

    public TraverseSetting() {
    }

    public static TraverseSetting load(String fileName) throws IOException {
        return load(new FileReader(fileName));
    }

    public static TraverseSetting load(Reader input) throws IOException {
        final Yaml yaml = new Yaml();
        return yaml.loadAs(input, TraverseSetting.class);
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public String getOutputDirectory() {
        return this.outputDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public long getScreenShotLimit() {
        return this.screenShotLimit;
    }

    public void setScreenShotLimit(long screenShotLimit) {
        this.screenShotLimit = screenShotLimit;
    }

    public long getInitialSleep() {
        return initialSleep;
    }

    public void setInitialSleep(long initialSleep) {
        this.initialSleep = initialSleep;
    }

    public long getSleep() {
        return sleep;
    }

    public void setSleep(long sleep) {
        this.sleep = sleep;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean getRecursively() {
        return recursively;
    }

    public void setRecursively(boolean recursively) {
        this.recursively = recursively;
    }

    public List<String> getSeeds() {
        return this.seeds;
    }

    public void setSeeds(List<String> seeds) {
        this.seeds = seeds;
    }

    public List<String> getAccessibleDomains() {
        return this.accessibleDomains;
    }

    public void setAccessibleDomains(List<String> accessibleDomains) {
        this.accessibleDomains = accessibleDomains;
    }

    public List<String> getExtractableDomains() {
        return this.extractableDomains;
    }

    public void setExtractableDomains(List<String> extractableDomains) {
        this.extractableDomains = extractableDomains;
    }

    public List<String> getExcludePaths() {
        return this.excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }

    public boolean containsExcludePaths(URL url) {
        if (this.getExcludePaths().isEmpty()) {
            return true;
        }

        boolean found = false;
        for (String path : this.getExcludePaths()) {
            if (url.getPath().startsWith(path)) {
                found = true;
                break;
            }
        }
        return found;
    }

    public List<String> getAllowSuffixes() {
        return this.allowSuffixes;
    }

    public void setAllowSuffixes(List<String> allowSuffixes) {
        this.allowSuffixes = allowSuffixes;
    }

    public static String removeFragmentPart(String path) {
        if (path == null) {
            return path;
        }

        final int index = path.indexOf('#');
        if (index >= 0) {
            return path.substring(index);
        }

        return path;
    }

    public static String removeQueryPart(String path) {
        if (path == null) {
            return path;
        }

        final int index = path.indexOf('?');
        if (index >= 0) {
            return path.substring(index);
        }

        return path;
    }

    public static String extractSuffix(URL url) {
        final String path = url.getPath();
        final int last_slash = path.lastIndexOf('/');

        final int last_dot = path.lastIndexOf('.');
        final String suffix;
        if (last_dot >= 0 && last_dot > last_slash) {
            suffix = path.substring(last_dot);
        } else {
            suffix = null;
        }

        return removeQueryPart(removeFragmentPart(suffix));
    }

    public boolean containsAllowSuffixes(URL url) {
        if (this.getAllowSuffixes().isEmpty()) {
            return true;
        }

        final String suffix = extractSuffix(url);
        return this.getAllowSuffixes().contains(suffix);
    }

    public boolean allowsForAccess(URL url) {
        if (!this.getAccessibleDomains().contains(url.getHost()) && !this.getAccessibleDomains().contains(url.getHost() + ":" + url.getPort())) {
            return false;
        }
        if (containsExcludePaths(url)) {
            return false;
        }

        return true;
    }

    public boolean allowsForExtraction(URL url) {
        if (!this.getExtractableDomains().contains(url.getHost())) {
            if (!this.getExtractableDomains().contains(url.getHost() + ":" + url.getPort())){
                LOGGER.debug("not included in extractable domains: {}", url);
                return false;
            }
        }
        if (containsExcludePaths(url)) {
            LOGGER.debug("excluded by paths: {}", url);
            return false;
        }
        if (!containsAllowSuffixes(url)) {
            LOGGER.debug("excluded by suffixes: {}", url);
            return false;
        }

        return true;
    }

    public String getPngQuant() {
        return this.pngQuant;
    }

    public void setPngQuant(String pngQuant) {
        this.pngQuant = pngQuant;
    }
}
