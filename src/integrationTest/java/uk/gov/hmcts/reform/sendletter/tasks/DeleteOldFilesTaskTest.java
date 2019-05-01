package uk.gov.hmcts.reform.sendletter.tasks;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sendletter.helper.FtpHelper;
import uk.gov.hmcts.reform.sendletter.services.LocalSftpServer;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileToSend;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(SpringExtension.class)
class DeleteOldFilesTaskTest {

    @Mock ServiceFolderMapping serviceFolderMapping;

    @Test
    void should_delete_files() throws Exception {
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
                return null;
            });

            assertThat(ftp.listLetters(LocalSftpServer.SERVICE_FOLDER)).hasSize(1); // sanity check

            // when
            new DeleteOldFilesTask(ftp, serviceFolderMapping, Duration.ZERO).run();

            //then
            assertThat(ftp.listLetters(LocalSftpServer.SERVICE_FOLDER)).isEmpty();
        }
    }
}
