package eu.tivian.other;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;

/**
 * Custom text logger.
 *
 * @author Paweł Kania
 * @since 2019-12-20
 * @see java.util.logging.Logger
 */
public class Logger {
    /**
     * Dictates if the logger will be activated.
     */
    public static boolean ENABLE = false;
    /**
     * If any of the ignored methods occurs in stack trace then logging shouldn't happen.
     */
    public static final Set<String> ignoreMethods = new HashSet<>();

    /**
     * Logger object.
     */
    private static final java.util.logging.Logger log;

    static {
        log = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "[%1$tT] [%4$s] %5$s %n");

        ignoreMethods.add("log");
    }

    /**
     * Returns if the call to the logger should be ignored.
     * @return {@code true} if the call to the logger should be ignored
     */
    private static boolean ignore() {
        for (var trace : Thread.currentThread().getStackTrace()) {
            if (ignoreMethods.contains(trace.getMethodName()))
                return true;
        }

        return false;
    }

    /**
     * Redirects output of the logger to the given file and disables console output.
     *
     * @param path path to the file for logging
     * @return {@code true} if redirection was successful
     */
    public static boolean redirect(String path) {
        try {
            var fileHandler = new FileHandler(path);
            log.addHandler(fileHandler);
            var formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            log.setUseParentHandlers(false);

            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    /**
     * Reports informative message.
     * @param msg message to log
     */
    public static void info(String msg) {
        log.info(msg);
    }

    /**
     * Reports warning level message.
     * @param msg message to log
     */
    public static void warn(String msg) {
        if (!ignore())
            log.warning(msg);
    }

    /**
     * Reports error level message.
     * @param msg message to log
     */
    public static void error(String msg) {
        if (!ignore())
            log.severe(msg);
    }
}
