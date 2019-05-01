package uk.gov.hmcts.reform.sendletter.tasks;

import net.schmizz.sshj.sftp.SFTPClient;
import org.assertj.core.util.Lists;
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
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.ftp.FileToSend;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.time.LocalTime;
import java.util.Arrays;
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

    @Mock
    private AppInsights insights;

    private ArgumentCaptor<FileToSend> captureFileToSend = ArgumentCaptor.forClass(FileToSend.class);

    @Captor
    private ArgumentCaptor<Function<SFTPClient, Integer>> captureRunWith;

    private UploadLettersTask task;

    @BeforeEach
    void setUp() {
        given(availabilityChecker.isFtpAvailable(any(LocalTime.class))).willReturn(true);
        given(ftpClient.runWith(any())).willReturn(0);// otherwise you'll face infinite loop

        this.task = new UploadLettersTask(repo, ftpClient, availabilityChecker, serviceFolderMapping, insights);
    }

    @AfterEach
    void tearDown() {
        reset(availabilityChecker, repo);
    }

    @Test
    void should_handle_smoke_test_letters() {
        // given
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        givenDbContains(
            letterOfType(SMOKE_TEST_LETTER_TYPE),
            letterOfType("not_" + SMOKE_TEST_LETTER_TYPE)
        );

        // when
        task.run();

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

        task.run();

        verify(ftpClient, never()).runWith(any());
        verify(repo, never()).findByStatus(eq(Created));
    }

    @Test
    void should_skip_letter_if_folder_for_its_service_is_not_configured() {
        // given
        givenDbContains(
            letterForService("service_A"),
            letterForService("service_B"),
            letterForService("service_C")
        );

        // and
        given(serviceFolderMapping.getFolderFor(eq("service_A"))).willReturn(Optional.of("folder_A"));
        given(serviceFolderMapping.getFolderFor(eq("service_B"))).willReturn(Optional.empty());
        given(serviceFolderMapping.getFolderFor(eq("service_C"))).willReturn(Optional.of("folder_C"));

        // when
        task.run();

        // and
        verify(ftpClient).runWith(captureRunWith.capture());

        // when
        int uploadAttempts = captureRunWith
            .getAllValues()
            .stream()
            .mapToInt(function -> function.apply(sftpClient))
            .sum();

        // then
        assertThat(uploadAttempts).isEqualTo(3);

        // and
        verify(ftpClient).upload(any(), eq("folder_A"), any());
        verify(ftpClient).upload(any(), eq("folder_C"), any());
        verifyNoMoreInteractions(ftpClient);
    }

    private Letter letterOfType(String type) {
        return new Letter(
            UUID.randomUUID(),
            "msgId",
            "cmc",
            null,
            type,
            "hello".getBytes(),
            true,
            now()
        );
    }

    private Letter letterForService(String serviceName) {
        return new Letter(
            UUID.randomUUID(),
            "msgId",
            serviceName,
            null,
            "type",
            "hello".getBytes(),
            true,
            now()
        );
    }

    @SuppressWarnings("unchecked")
    private void givenDbContains(Letter... letters) {
        // Return letter on first call, then empty list.
        given(repo.findFirst10ByStatus(eq(Created)))
            .willReturn(Arrays.asList(letters)).willReturn(Lists.newArrayList());
    }
}
