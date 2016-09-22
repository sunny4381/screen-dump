package org.ssproj;

public class TraverserFactory {
    public static Traverser create(TraverserContext context) {
        final String browser = context.getBrowser();
        final Traverser traverser;
        if (browser.equalsIgnoreCase("firefox")) {
            traverser = new FirefoxTraverser(context);
        } else if (browser.equalsIgnoreCase("chrome")) {
            traverser = new ChromeTraverser(context);
        } else if (browser.equalsIgnoreCase("ie")) {
            traverser = new InternetExplorerTraverser(context);
        } else if (browser.equalsIgnoreCase("edge")) {
            traverser = new EdgeTraverser(context);
        } else if (browser.equalsIgnoreCase("safari")) {
            traverser = new SafariTraverser(context);
        } else {
            throw new IllegalArgumentException("unknown browser");
        }

        return traverser;
    }
}
