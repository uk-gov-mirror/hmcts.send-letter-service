package uk.gov.hmcts.reform.sendletter.services;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.sftp.SFTPFileTransfer;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.userauth.UserAuthException;
import net.schmizz.sshj.userauth.keyprovider.KeyProvider;
import net.schmizz.sshj.xfer.LocalSourceFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.config.FtpConfigProperties;
import uk.gov.hmcts.reform.sendletter.exception.FtpException;
import uk.gov.hmcts.reform.sendletter.model.Report;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileToSend;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.RetryOnExceptionStrategy;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.doNothing;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FtpClientTest {

    private FtpClient client;

    @Mock private SSHClient sshClient;
    @Mock private SFTPClient sftpClient;
    @Mock private SFTPFileTransfer sftpFileTransfer;
    @Mock private FtpConfigProperties ftpProps;
    @Spy private RetryOnExceptionStrategy retry;


    @BeforeEach
    void setUp() {
        retry = new RetryOnExceptionStrategy(2, 2000);
        client = new FtpClient(() -> sshClient, ftpProps, retry);
    }

    @Test
    void download_should_not_include_non_csv_files() throws Exception {
        // given
        given(sshClient.newSFTPClient()).willReturn(sftpClient);
        given(sftpClient.getFileTransfer()).willReturn(sftpFileTransfer);
        RemoteResourceInfo nonCsvFile = mock(RemoteResourceInfo.class);
        given(nonCsvFile.isRegularFile()).willReturn(true);
        given(nonCsvFile.getName()).willReturn("test-report.pdf");
        given(ftpProps.getReportsFolder()).willReturn("reports");
        given(sftpClient.ls(anyString()))
            .willReturn(singletonList(nonCsvFile));

        // when
        List<Report> reports = client.downloadReports();

        // then
        assertThat(reports).isEmpty();
    }

    @Test
    void should_track_failure_when_trying_to_download_report() throws IOException {
        // given
        given(sshClient.newSFTPClient()).willReturn(sftpClient);
        given(sftpClient.getFileTransfer()).willReturn(sftpFileTransfer);
        willThrow(IOException.class).given(sftpClient).ls(eq(null));

        // when
        Throwable exception = catchThrowable(() -> client.downloadReports());

        // then
        assertThat(exception).isInstanceOf(FtpException.class);
    }

    @Test
    void should_upload_file_to_correct_folder_based_on_whether_its_a_smoke_test_or_not() throws Exception {
        given(sftpClient.getFileTransfer()).willReturn(sftpFileTransfer);
        given(ftpProps.getSmokeTestTargetFolder()).willReturn("smoke");
        given(ftpProps.getTargetFolder()).willReturn("regular");

        // when
        client.upload(new FileToSend("hello.zip", "hello".getBytes(), true), "cmc", sftpClient);

        // then
        verify(sftpFileTransfer)
            .upload(
                any(LocalSourceFile.class),
                contains("smoke") //path
            );

        // when
        client.upload(new FileToSend("hello.zip", "hello".getBytes(), false), "cmc", sftpClient);

        // then
        verify(sftpFileTransfer)
            .upload(
                any(LocalSourceFile.class),
                contains("regular") //path
            );
    }

    @Test
    void should_track_failure_when_trying_to_upload_new_file() throws IOException {
        // given
        given(sftpClient.getFileTransfer()).willReturn(sftpFileTransfer);
        willThrow(IOException.class).given(sftpFileTransfer).upload(any(LocalSourceFile.class), anyString());

        // when
        Throwable exception = catchThrowable(() ->
            client.upload(new FileToSend("goodbye.zip", "goodbye".getBytes(), false), "cmc", sftpClient)
        );

        // then
        assertThat(exception).isInstanceOf(FtpException.class);
    }

    @Test
    void should_delete_corrupt_file_in_case_upload_timed_out() throws IOException {
        // given
        given(sftpClient.getFileTransfer()).willReturn(sftpFileTransfer);
        willThrow(new IOException(new TimeoutException("oh no")))
            .given(sftpFileTransfer)
            .upload(any(LocalSourceFile.class), anyString());

        // when
        Throwable exception = catchThrowable(() ->
            client.upload(new FileToSend("massive.zip", "insane size".getBytes(), false), "cmc", sftpClient)
        );

        // then
        assertThat(exception)
            .isInstanceOf(FtpException.class)
            .hasMessageStartingWith("Unable to upload file");
        verify(sftpClient, times(2)).rm("null/cmc/massive.zip"); // we mocked mapping hence null
    }

    @Test
    void should_delete_report_from_ftp() throws IOException {
        // given
        given(sshClient.newSFTPClient()).willReturn(sftpClient);
        doNothing().when(sftpClient).rm(anyString());

        // when
        Throwable exception = catchThrowable(() -> client.deleteReport("some/report"));

        // then
        assertThat(exception).isNull();
    }

    @Test
    void should_track_failure_when_trying_to_delete_report_from_ftp() throws IOException {
        // given
        given(sshClient.newSFTPClient()).willReturn(sftpClient);
        willThrow(IOException.class).given(sftpClient).rm(anyString());

        // when
        Throwable exception = catchThrowable(() -> client.deleteReport("some/report"));

        // then
        assertThat(exception).isInstanceOf(FtpException.class);
    }

    @Test
    void should_not_apply_action_when_failed_to_authenticate_user() throws UserAuthException, TransportException {
        // given (nulls because mocks)
        willThrow(new UserAuthException("oh no", new TimeoutException())).given(sshClient).authPublickey(
            null,
            (KeyProvider) null
        );
        AtomicBoolean actuallyNotModified = new AtomicBoolean(true);

        // when
        Throwable exception = catchThrowable(() -> client.runWith(sftpClient -> actuallyNotModified.getAndSet(false)));

        // then
        assertThat(actuallyNotModified).isTrue();
        assertThat(exception)
            .isInstanceOf(FtpException.class)
            .hasMessage("Unable to authenticate.")
            .hasCauseInstanceOf(UserAuthException.class);
    }

    @Test
    void should_track_failure_when_trying_to_upload_new_file_retry() throws IOException {
        // given
        given(sftpClient.getFileTransfer()).willReturn(sftpFileTransfer);
        willThrow(IOException.class).given(sftpFileTransfer).upload(any(LocalSourceFile.class), anyString());

        // when
        Throwable exception = catchThrowable(() ->
                client.upload(new FileToSend("goodbye.zip", "goodbye".getBytes(), false), "cmc", sftpClient)
        );

        // then
        assertThat(exception).isInstanceOf(FtpException.class);
    }
}
