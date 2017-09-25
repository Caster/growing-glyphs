package logging;

import java.util.logging.ConsoleHandler;
import java.util.logging.Filter;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

/**
 * This class implements what the {@link ConsoleHandler} should have implemented.
 * It is the exact same as that class, but additionally makes it possible to
 * configure the output stream that log messages should be sent to.
 */
public class ConfigurableConsoleHandler extends StreamHandler {

    private String indent = "";


    public ConfigurableConsoleHandler() {
        configure();
    }

    @Override
    public synchronized void publish(LogRecord record) {
        if (record.getLevel().intValue() <= Level.FINE.intValue()) {
            int l = (500 - record.getLevel().intValue()) / 100;
            StringBuilder msg = new StringBuilder(
                    record.getMessage().length() + l * indent.length());
            for (int i = 0; i < l; ++i) {
                msg.append(indent);
            }
            msg.append(record.getMessage());
            record.setMessage(msg.toString());
        }
        super.publish(record);
        flush();
    }


    /**
     * Attempt to instantiate a class from a given name.
     *
     * @param name Name (including packages) of class to instantiate.
     */
    private Object classFromName(String name) {
        try {
            if (name != null) {
                Class<?> klass = ClassLoader.getSystemClassLoader().loadClass(name);
                return klass.newInstance();
            }
        } catch (Exception ex) {
            // fall through
        }
        return null;
    }

    /**
     * Same type of private method as {@link ConsoleHandler} has, but also allow
     * to configure the output stream.
     */
    private void configure() {
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();

        // set log level
        try {
            setLevel(Level.parse(manager.getProperty(cname + ".level")));
        } catch (IllegalArgumentException|NullPointerException ex) {
            setLevel(Level.INFO); // set to default log level
        }

        // set filter
        Object filter = classFromName(manager.getProperty(cname + ".filter"));
        if (filter != null && filter instanceof Filter) {
            setFilter((Filter) filter);
        } else {
            setFilter(null); // default value
        }

        // set formatter
        Object formatter = classFromName(manager.getProperty(cname + ".formatter"));
        if (formatter != null && formatter instanceof Formatter) {
            setFormatter((Formatter) formatter);
        } else {
            setFormatter(new SimpleFormatter()); // default value
        }

        // set encoding
        try {
            setEncoding(manager.getProperty(cname + ".encoding"));
        } catch (Exception ex) {
            try {
                setEncoding(null);
            } catch (Exception ex2) {
                // doing a setEncoding with null should always work, ignore this
            }
        }

        // set output stream
        if ("out".equals(manager.getProperty(cname + ".stream"))) {
            setOutputStream(new NonClosablePrintStream(System.out));
        } else {
            setOutputStream(new NonClosablePrintStream(System.err));
        }

        // set whether FINE/FINER/FINEST messages should be indented
        indent = manager.getProperty(cname + ".indent");
        if (indent == null) {
            indent = "";
        } else {
            indent = indent.replaceAll("\\.", " ");
        }
    }

}
