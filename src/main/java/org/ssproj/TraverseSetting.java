package org.ssproj;

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

    public boolean containsAllowSuffixes(URL url) {
        boolean found = false;
        for (String suffix : this.getAllowSuffixes()) {
            if (url.getPath().endsWith(suffix)) {
                found = true;
                break;
            }
        }
        return found;
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
        if (!this.getExtractableDomains().contains(url.getHost()) && !this.getExtractableDomains().contains(url.getHost() + ":" + url.getPort())) {
            return false;
        }
        if (containsExcludePaths(url)) {
            return false;
        }
        if (!containsAllowSuffixes(url)) {
            return false;
        }

        return true;
    }
}
