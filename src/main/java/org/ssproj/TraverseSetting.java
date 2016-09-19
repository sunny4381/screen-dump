package org.ssproj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by nakano on 2016/05/13.
 */
public class TraverseSetting {
    private final static Logger LOGGER = LoggerFactory.getLogger(TraverseSetting.class);
    public static TraverseSetting load(String fileName) throws IOException {
        Yaml yaml = new Yaml();
        HashMap setting = (HashMap) yaml.load(new FileReader(fileName));

        return new TraverseSetting((HashMap) setting.get("traverse"));
    }

    private final HashMap settings;
    private List<String> seeds;
    private List<String> accessibleDomains;
    private List<String> extractableDomains;
    private List<String> excludePaths;
    private List<String> allowSuffixes;
    private String pngQuant;

    public TraverseSetting(HashMap settings) {
        this.settings = settings;
    }

    public List<String> getSeeds() {
        if (this.seeds == null) {
            this.seeds = (List) this.settings.getOrDefault("seeds", Collections.<String>emptyList());
        }
        return this.seeds;
    }

    public List<String> getAccessibleDomains() {
        if (this.accessibleDomains == null) {
            this.accessibleDomains = (List) this.settings.getOrDefault("accessible_domains", Collections.<String>emptyList());
        }
        return this.accessibleDomains;
    }

    public List<String> getExtractableDomains() {
        if (this.extractableDomains == null) {
            this.extractableDomains = (List) this.settings.getOrDefault("extractable_domains", Collections.<String>emptyList());
        }
        return this.extractableDomains;
    }

    public List<String> getExcludePaths() {
        if (this.excludePaths == null) {
            this.excludePaths = (List) this.settings.getOrDefault("exclude_paths", Collections.<String>emptyList());
        }
        return this.excludePaths;
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
        if (this.allowSuffixes == null) {
            this.allowSuffixes = (List) this.settings.getOrDefault("allow_suffixes", Collections.<String>emptyList());
        }
        return this.allowSuffixes;
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
        if (this.pngQuant == null) {
            this.pngQuant = (String) this.settings.getOrDefault("pngquant", null);
        }
        return this.pngQuant;
    }
}
