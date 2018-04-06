package uk.gov.hmcts.reform.sendletter.logging;

/**
 * Used to track dependency of how long in millis did it take to perform a command.
 */
final class AppDependency {

    /**
     * Service auth provider.
     */
    static final String AUTH_SERVICE = "AuthService";

    /**
     * Ftp client.
     */
    static final String FTP_CLIENT = "FtpClient";

    private AppDependency() {
        // utility class constructor
    }
}
