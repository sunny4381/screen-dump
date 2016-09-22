package org.ssproj;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;

public class Main {
    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, ParseException {
        final Options opts = new Options();
        opts.addOption("t", "traverse", true, "specify traverse.yml");
        opts.addOption("r", "recursive", false, "specify traverse other links recursively.");
        opts.addOption("b", "browser", true, "specify one of browser: ie, firefox, chrome, edge. default is `ie`.");
        opts.addOption("c", "concurrency", true, "specify concurrency. default is 2.");
        opts.addOption("o", "output", true, "specify output directory. default is `screenshots`.");
        opts.addOption("l", "limit", true, "specify screen shots limits. default is 0 (unlimited).");
        opts.addOption("i", "initial-sleep", true, "specify sleep in milliseconds before taking first screenshot. default is 5000.");
        opts.addOption("s", "sleep", true, "specify sleep in milliseconds before taking each screenshots. default is 500.");
        opts.addOption("width", "width", true, "specify window width.");
        opts.addOption("height", "height", true, "specify window height.");

        final CommandLineParser parser = new DefaultParser();
        final CommandLine cl;
        try {
            cl = parser.parse(opts, args);
        } catch (ParseException e) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("screen-dump", opts, true);
            return;
        }

        final TraverserContext context;
        if (cl.hasOption('t')) {
            String fileName = cl.getOptionValue('t');
            try {
                context = TraverserContext.load(fileName);
            } catch (YAMLException e) {
                LOGGER.error(fileName + ": YAML load error", e);
                return;
            }
        } else {
            context = new TraverserContext();
        }

        if (cl.hasOption('r')) {
            context.setRecursively(true);
        }

        if (cl.hasOption('b')) {
            context.setBrowser(cl.getOptionValue('b'));
        }

        if (cl.hasOption('c')) {
            context.setConcurrency(Integer.parseInt(cl.getOptionValue('c')));
        }

        if (cl.hasOption('o')) {
            context.setOutputDirectory(cl.getOptionValue('o'));
        }

        if (cl.hasOption('l')) {
            context.setScreenShotLimit(Long.parseLong(cl.getOptionValue('l')));
        }

        if (cl.hasOption('i')) {
            context.setInitialSleep(Long.parseLong(cl.getOptionValue('i')));
        }

        if (cl.hasOption('s')) {
            context.setSleep(Long.parseLong(cl.getOptionValue('s')));
        }

        if (cl.hasOption("width")) {
            context.setWidth(Integer.parseInt(cl.getOptionValue("width")));
        }

        if (cl.hasOption("height")) {
            context.setHeight(Integer.parseInt(cl.getOptionValue("height")));
        }

        TraverserDriver.run(context, cl.getArgList());
    }
}
