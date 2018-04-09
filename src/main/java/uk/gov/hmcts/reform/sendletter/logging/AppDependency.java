package uk.gov.hmcts.reform.sendletter.logging;

/**
 * Used to track dependency of how long in millis did it take to perform a command.
 */
public final class AppDependency {

    /**
     * Service auth provider.
     */
    public static final String AUTH_SERVICE = "AuthService";

    /**
     * Ftp client.
     */
    public static final String FTP_CLIENT = "FtpClient";

    private AppDependency() {
        // utility class constructor
    }
}
