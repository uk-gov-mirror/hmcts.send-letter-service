package uk.gov.hmcts.reform.sendletter.tasks;

import net.schmizz.sshj.sftp.SFTPClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileToSend;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Skipped;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Uploaded;
import static uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask.SMOKE_TEST_LETTER_TYPE;

@ExtendWith(MockitoExtension.class)
class UploadLettersTaskTest {

    @Mock
    private LetterRepository repo;

    @Mock
    private FtpClient ftpClient;

    @Mock
    private SFTPClient sftpClient;

    @Mock
    private FtpAvailabilityChecker availabilityChecker;

    @Mock
    private ServiceFolderMapping serviceFolderMapping;

    private ArgumentCaptor<FileToSend> captureFileToSend = ArgumentCaptor.forClass(FileToSend.class);

    @Captor
    private ArgumentCaptor<Function<SFTPClient, Integer>> captureRunWith;

    @BeforeEach
    void setUp() {
        given(availabilityChecker.isFtpAvailable(any(LocalTime.class))).willReturn(true);
        given(ftpClient.runWith(any())).willReturn(0);// value is a counter of uploaded letters
    }

    @AfterEach
    void tearDown() {
        reset(availabilityChecker, repo);
    }

    @Test
    void should_handle_smoke_test_letters() {
        // given
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));

        given(repo.countByStatus(eq(Created))).willReturn(2);

        given(repo.findFirstByStatusOrderByCreatedAtAsc(eq(Created)))
            .willReturn(Optional.of(letterOfType(SMOKE_TEST_LETTER_TYPE)))
            .willReturn(Optional.of(letterOfType("not_" + SMOKE_TEST_LETTER_TYPE)))
            .willReturn(Optional.empty());

        // when
        task().run();

        // and
        verify(ftpClient).runWith(captureRunWith.capture());

        // when
        int uploadAttempts = captureRunWith
            .getAllValues()
            .stream()
            .mapToInt(function -> function.apply(sftpClient))
            .sum();

        // then
        assertThat(uploadAttempts).isEqualTo(2);

        // and
        verify(ftpClient, times(2)).upload(captureFileToSend.capture(), any(), any());
        assertThat(
            captureFileToSend
                .getAllValues()
                .stream()
                .map(file -> file.isSmokeTest)
        ).containsExactlyInAnyOrder(false, true);
    }

    @Test
    void should_not_start_process_if_ftp_is_not_available() {
        reset(availabilityChecker, ftpClient);
        given(availabilityChecker.isFtpAvailable(any(LocalTime.class))).willReturn(false);

        task().run();

        verify(ftpClient, never()).runWith(any());
        verify(repo, never()).countByStatus(eq(Created));
    }

    @Test
    void should_skip_letter_if_folder_for_its_service_is_not_configured() {
        // given
        Letter letterA = letterForService("service_A");
        Letter letterB = letterForService("service_B");
        Letter letterC = letterForService("service_C");

        given(repo.countByStatus(eq(Created))).willReturn(3);

        given(repo.findFirstByStatusOrderByCreatedAtAsc(eq(Created)))
            .willReturn(Optional.of(letterA))
            .willReturn(Optional.of(letterB))
            .willReturn(Optional.of(letterC))
            .willReturn(Optional.empty());

        // and
        given(serviceFolderMapping.getFolderFor(letterA.getService())).willReturn(Optional.of("folder_A"));
        given(serviceFolderMapping.getFolderFor(letterB.getService())).willReturn(Optional.empty());
        given(serviceFolderMapping.getFolderFor(letterC.getService())).willReturn(Optional.of("folder_C"));

        // when
        task().run();

        // and
        verify(ftpClient).runWith(captureRunWith.capture());

        // when
        int uploadAttempts = captureRunWith
            .getAllValues()
            .stream()
            .mapToInt(function -> function.apply(sftpClient))
            .sum();

        // then
        assertThat(uploadAttempts).isEqualTo(2);
        assertThat(letterA.getStatus()).isEqualTo(Uploaded);
        assertThat(letterB.getStatus()).isEqualTo(Skipped);
        assertThat(letterC.getStatus()).isEqualTo(Uploaded);

        // and
        verify(ftpClient).upload(any(), eq("folder_A"), any());
        verify(ftpClient).upload(any(), eq("folder_C"), any());
        verifyNoMoreInteractions(ftpClient);
    }

    private Letter letterOfType(String type) {
        return letter("cmc", type);
    }

    private Letter letterForService(String serviceName) {
        return letter(serviceName, "type");
    }

    private Letter letter(String service, String type) {
        return new Letter(
            UUID.randomUUID(),
            "msgId",
            service,
            null,
            type,
            "hello".getBytes(),
            true,
            "9c61b7da4e6c94416be51136122ed01acea9884f",
            now()
        );
    }

    private UploadLettersTask task() {
        return new UploadLettersTask(
            repo,
            ftpClient,
            availabilityChecker,
            serviceFolderMapping
        );
    }
}
