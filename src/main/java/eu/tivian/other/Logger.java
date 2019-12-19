package eu.tivian.other;

import java.util.HashSet;
import java.util.Set;

public class Logger {
    public static final boolean ENABLE = true;
    public static final Set<String> ignoreMethods = new HashSet<>();

    private static final java.util.logging.Logger log;
    private static Exception ex = null;

    static {
        log = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "[%1$tT] [%4$s] %5$s %n");

        ignoreMethods.add("log");
    }

    private static boolean ignore() {
        for (var trace : Thread.currentThread().getStackTrace()) {
            if (ignoreMethods.contains(trace.getMethodName()))
                return true;
        }

        return false;
    }

    public static void info(String msg) {
        log.info(msg);
    }

    public static void warn(String msg) {
        if (!ignore())
            log.warning(msg);
    }

    public static void error(String msg) {
        if (!ignore())
            log.severe(msg);
    }
}
