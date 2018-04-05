package uk.gov.hmcts.reform.sendletter.logging;

final class AppDependencyCommand {

    /**
     * File uploaded to ftp.
     */
    static final String FTP_FILE_UPLOADED = "FtpFileUploaded";

    /**
     * Report of printed documents has been downloaded from ftp.
     */
    static final String FTP_DOWNLOAD_REPORTS = "FtpDownloadedReports";

    /**
     * Report of printed documents has been deleted from ftp.
     */
    static final String FTP_REPORT_DELETE = "FtpReportDeleted";

    private AppDependencyCommand() {
        // utility class constructor
    }
}
