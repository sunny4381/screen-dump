package org.ssproj;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

public class InternetExplorerTraverser extends Traverser {
    public InternetExplorerTraverser(TraverserContext context) {
        super(context);
    }

    @Override
    protected WebDriver startDriver() {
        LOGGER.info("starting internet explorer driver");
        return new InternetExplorerDriver();
    }
}
