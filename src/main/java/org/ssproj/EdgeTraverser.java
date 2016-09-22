package org.ssproj;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.edge.EdgeDriver;

public class EdgeTraverser extends Traverser {
    public EdgeTraverser(TraverserContext context) {
        super(context);
    }

    protected WebDriver startDriver() {
        LOGGER.info("starting edge driver");
        return new EdgeDriver();
    }
}
