package org.ssproj;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Href2URI {
    private final static Logger LOGGER = LoggerFactory.getLogger(Href2URI.class);
    private final WebDriver driver;
    private final URI baseURI;

    public Href2URI(WebDriver driver) throws URISyntaxException {
        this.driver = driver;
        this.baseURI = new URI(this.driver.getCurrentUrl());
    }

    public Object getBaseURI() {
        return baseURI;
    }

    public URL apply(String href) {
        URI uri = baseURI.resolve(href).normalize();

        try {
            // remove fragmet
            return new URI(uri.getScheme(), uri.getUserInfo(),
                    uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), null).toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            LOGGER.debug(href + ": malformed uri", e);
            return null;
        }
    }

    public List<URL> apply(List<String> list) {
        ArrayList<URL> ret = new ArrayList<>();

        for (String href : list) {
            URL uri = apply(href);
            if (uri != null) {
                ret.add(uri);
            }
        }

        return ret;
    }
}
