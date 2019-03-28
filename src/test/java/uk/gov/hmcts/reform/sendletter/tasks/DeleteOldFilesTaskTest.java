package uk.gov.hmcts.reform.sendletter.tasks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.exception.FtpException;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileInfo;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeleteOldFilesTaskTest {

    @Mock private FtpClient ftp;
    @Mock private ServiceFolderMapping serviceFolderMapping;

    @Test
    void should_remove_only_files_that_are_old_enough() throws Exception {
        // given
        Duration ttl = Duration.ofMinutes(1);

        given(serviceFolderMapping.getFolders())
            .willReturn(ImmutableSet.of("SERVICE"));

        given(ftp.listLetters("SERVICE"))
            .willReturn(asList(
                new FileInfo("new.zip", now()),
                new FileInfo("old.zip", now().minus(ttl.plusSeconds(1))),
                new FileInfo("almostOld.zip", now().minus(ttl.minusSeconds(1)))
            ));

        // when
        new DeleteOldFilesTask(ftp, serviceFolderMapping, ttl).run();

        // then
        verify(ftp).deleteFile("old.zip");
        verify(ftp, never()).deleteFile("new.zip");
        verify(ftp, never()).deleteFile("almostOld.zip");
    }

    @Test
    void should_delete_files_for_all_known_services() throws Exception {
        // given
        given(serviceFolderMapping.getFolders())
            .willReturn(ImmutableSet.of(
                "A",
                "B"
            ));

        given(ftp.listLetters("A")).willReturn(ImmutableList.of(new FileInfo("a.zip", secondAgo())));
        given(ftp.listLetters("B")).willReturn(ImmutableList.of(new FileInfo("b.zip", secondAgo())));

        // when
        new DeleteOldFilesTask(ftp, serviceFolderMapping, Duration.ZERO).run();

        // then
        verify(ftp).deleteFile("a.zip");
        verify(ftp).deleteFile("b.zip");
    }

    @Test
    void should_try_to_delete_next_file_if_previous_failed() throws Exception {
        // given
        given(serviceFolderMapping.getFolders())
            .willReturn(ImmutableSet.of("SERVICE"));

        List<FileInfo> files = asList(
            new FileInfo("error1.zip", secondAgo()),
            new FileInfo("error2.zip", secondAgo()),
            new FileInfo("ok1.zip", secondAgo()),
            new FileInfo("ok2.zip", secondAgo())
        );

        given(ftp.listLetters("SERVICE")).willReturn(files);

        willThrow(FtpException.class).given(ftp).deleteFile("error1.zip");
        willThrow(FtpException.class).given(ftp).deleteFile("error2.zip");

        // when
        new DeleteOldFilesTask(ftp, serviceFolderMapping, Duration.ZERO).run();

        // then
        files.forEach(file -> verify(ftp).deleteFile(file.path));
    }

    private Instant secondAgo() {
        return now().minusSeconds(1);
    }
}