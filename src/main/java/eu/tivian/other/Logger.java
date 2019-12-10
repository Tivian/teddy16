package eu.tivian.other;

public class Logger {
    public static final boolean ENABLE = true;
    private static final java.util.logging.Logger log;

    static {
        log = java.util.logging.Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "[%1$tT] [%4$s] %5$s %n");
    }

    public static void info(String msg) {
        log.info(msg);
    }

    public static void warn(String msg) {
        log.warning(msg);
    }

    public static void error(String msg) {
        log.severe(msg);
    }
}
