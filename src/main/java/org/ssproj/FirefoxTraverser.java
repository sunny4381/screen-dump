package org.ssproj;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

public class FirefoxTraverser extends Traverser {
    public FirefoxTraverser(TraverserContext context) {
        super(context);
    }

    @Override
    protected WebDriver startDriver() {
        LOGGER.info("starting firefox driver");
        return new FirefoxDriver();
    }
}
