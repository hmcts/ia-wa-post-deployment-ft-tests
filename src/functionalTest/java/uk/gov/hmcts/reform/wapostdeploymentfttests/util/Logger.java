package uk.gov.hmcts.reform.wapostdeploymentfttests.util;

@SuppressWarnings("unchecked")
public final class Logger {

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLACK = "\u001B[30m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";

    private Logger() {
        // noop
    }

    public static void say(LoggerMessage message, Object content) {
        switch (message) {
            case SCENARIO_START:
                colouredPrintLine(ANSI_CYAN, "-------------------------------------------------------------------");
                colouredPrintLine(ANSI_YELLOW, "⚙️️ RUNNING " + content + " SCENARIOS");
                colouredPrintLine(ANSI_CYAN, "-------------------------------------------------------------------");
                break;
            case SCENARIO_DISABLED:
                colouredPrintLine(ANSI_RED, "ℹ️ SCENARIO: " + content + " **disabled**");
                break;
            case SCENARIO_ENABLED:
                colouredPrintLine(ANSI_YELLOW, "ℹ️ SCENARIO " + content);
                break;
            case SCENARIO_FINISHED:
                colouredPrintLine(ANSI_CYAN, "-------------------------------------------------------------------");
                break;
            case SCENARIO_SUCCESSFUL:
                colouredPrintLine(ANSI_GREEN, "✅ SCENARIO: " + content + " completed successfully");
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + message);
        }

    }

    private static void colouredPrintLine(String ansiColour, String content) {
        System.out.println(ansiColour + content + ANSI_RESET);
    }

}
