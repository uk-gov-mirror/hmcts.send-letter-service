package uk.gov.hmcts.reform.sendletter.tasks;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sendletter.helper.FtpHelper;
import uk.gov.hmcts.reform.sendletter.services.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileToSend;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.time.Duration;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class DeleteOldFilesTaskTest {

    @Mock ServiceFolderMapping serviceFolderMapping;

    @Mock
    private FtpAvailabilityChecker availabilityChecker;

    @Test
    void should_delete_files() throws Exception {

        when(availabilityChecker.isFtpAvailable(any(LocalTime.class))).thenReturn(true);

        try (LocalSftpServer server = LocalSftpServer.create()) {
            // given
            given(serviceFolderMapping.getFolders())
                .willReturn(ImmutableSet.of(LocalSftpServer.SERVICE_FOLDER));

            FtpClient ftp = FtpHelper.getSuccessfulClient(LocalSftpServer.port);

            ftp.runWith(sftpClient -> {
                ftp.upload(
                    new FileToSend("hello.zip", "some content".getBytes(), false),
                    LocalSftpServer.SERVICE_FOLDER,
                    sftpClient
                );

                assertThat(ftp.listLetters(LocalSftpServer.SERVICE_FOLDER, sftpClient)).hasSize(1); // sanity check

                return null;
            });

            // when
            new DeleteOldFilesTask(ftp, serviceFolderMapping, Duration.ZERO, availabilityChecker).run();

            //then
            ftp.runWith(sftpClient -> {
                assertThat(ftp.listLetters(LocalSftpServer.SERVICE_FOLDER, sftpClient)).isEmpty();

                return null;
            });
        }
    }
}
