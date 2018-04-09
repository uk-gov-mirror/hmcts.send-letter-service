package uk.gov.hmcts.reform.sendletter.logging;

public final class AppDependencyCommand {

    /**
     * Authenticate service header and retrieve it's name.
     */
    public static final String AUTH_SERVICE_HEADER = "AuthenticateServiceHeader";

    /**
     * File uploaded to ftp.
     */
    public static final String FTP_FILE_UPLOADED = "FtpFileUploaded";

    /**
     * Report of printed documents has been downloaded from ftp.
     */
    public static final String FTP_DOWNLOAD_REPORTS = "FtpDownloadedReports";

    /**
     * Report of printed documents has been deleted from ftp.
     */
    public static final String FTP_REPORT_DELETE = "FtpReportDeleted";

    private AppDependencyCommand() {
        // utility class constructor
    }
}
