package sk.task.kafka;

// https://garbagecollected.org/2007/12/08/guice-debug-output/

import java.time.LocalTime;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;

/**
 * Enable or disable Guice debug output on the console.
 */
public class GuiceDebug {
    private static final Handler HANDLER;

    static {
        HANDLER = new ConsoleHandler();
        HANDLER.setFormatter(new Formatter() {
            public String format(LogRecord record) {
                return String.format("%s [Guice %s] %s%n",
                        LocalTime.now(),
                        record.getLevel().getName(),
                        record.getMessage());
            }
        });
        HANDLER.setLevel(Level.ALL);
    }

    private GuiceDebug() {
    }

    public static Logger getLogger() {
        return Logger.getLogger("com.google.inject");
    }

    public static void enable() {
        Logger guiceLogger = getLogger();
        guiceLogger.addHandler(GuiceDebug.HANDLER);
        guiceLogger.setLevel(Level.ALL);
    }

    public static void disable() {
        Logger guiceLogger = getLogger();
        guiceLogger.setLevel(Level.OFF);
        guiceLogger.removeHandler(GuiceDebug.HANDLER);
    }
}