package uk.gov.hmcts.reform.sendletter.logging;

public final class DependencyCommand {

    public static final String FTP_CONNECTED = "FtpConnected";

    public static final String FTP_FILE_DELETED = "FtpFileDeleted";

    public static final String FTP_FILE_UPLOADED = "FtpFileUploaded";

    public static final String FTP_LIST_FILES = "FtpListFiles";

    public static final String FTP_REPORT_DELETED = "FtpReportDeleted";

    public static final String FTP_REPORT_DOWNLOADED = "FtpReportDownloaded";

    private DependencyCommand() {
        // utility class constructor
    }
}
