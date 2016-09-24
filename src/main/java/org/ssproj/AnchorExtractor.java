package org.ssproj;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AnchorExtractor {
    private final static Logger LOGGER = LoggerFactory.getLogger(AnchorExtractor.class);
    private static final long STALE_ELEMENT_REFERENCE_WAIT = 5000;
    private WebDriver driver;

    public AnchorExtractor(WebDriver driver) {
        this.driver = driver;
    }

    public List<String> apply() throws InterruptedException {
        int retryCount = 0;

        for (;;) {
            try {
                return applyImpl();
            } catch (StaleElementReferenceException e) {
                retryCount++;
                if (retryCount >= 3) {
                    LOGGER.warn("give up retrying", e);
                    break;
                }

                Thread.sleep(STALE_ELEMENT_REFERENCE_WAIT);
            }
        }

        return Collections.emptyList();
    }

    private List<String> applyImpl() {
        ArrayList<String> hrefList = new ArrayList<>();

        for (WebElement element : this.driver.findElements(By.tagName("a"))) {
            final String href = element.getAttribute("href");
            if (href == null || href.isEmpty()) {
                continue;
            }

            hrefList.add(href);
        }

        return hrefList;
    }
}
