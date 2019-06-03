package uk.gov.hmcts.reform.sendletter.logging;

public final class DependencyCommand {

    public static final String FTP_FILE_UPLOADED = "FtpFileUploaded";

    public static final String FTP_REPORT_DELETED = "FtpReportDeleted";

    public static final String FTP_REPORT_DOWNLOADED = "FtpReportDownloaded";

    private DependencyCommand() {
        // utility class constructor
    }
}
