package org.ssproj;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.safari.SafariDriver;

public class SafariTraverser extends Traverser {
    public SafariTraverser(TraverserContext context) {
        super(context);
    }

    @Override
    protected WebDriver startDriver() {
        LOGGER.info("starting safari driver");
        return new SafariDriver();
    }
}
