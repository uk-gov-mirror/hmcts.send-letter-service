package uk.gov.hmcts.reform.sendletter.tasks;

import org.assertj.core.util.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.ftp.FtpClient;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask.SMOKE_TEST_LETTER_TYPE;

@RunWith(MockitoJUnitRunner.class)
public class UploadLettersTaskTest {

    @Mock
    private LetterRepository repo;
    @Mock
    private FtpClient ftpClient;
    @Mock
    private FtpAvailabilityChecker availabilityChecker;
    @Mock
    private ServiceFolderMapping serviceFolderMapping;
    @Mock
    private AppInsights insights;

    private UploadLettersTask task;

    @Before
    public void setUp() {
        given(availabilityChecker.isFtpAvailable(any(LocalTime.class))).willReturn(true);
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        this.task = new UploadLettersTask(repo, ftpClient, availabilityChecker, serviceFolderMapping, insights);
    }

    @After
    public void tearDown() {
        reset(availabilityChecker, repo);
    }

    @Test
    public void should_handle_smoke_test_letters() {

        givenDbContains(letterOfType(SMOKE_TEST_LETTER_TYPE));
        task.run();
        verify(ftpClient).upload(any(), eq(true), any());

        givenDbContains(letterOfType("not_" + SMOKE_TEST_LETTER_TYPE));
        task.run();
        verify(ftpClient).upload(any(), eq(false), any());
    }

    @Test
    public void should_not_start_process_if_ftp_is_not_available() {
        reset(availabilityChecker);
        given(availabilityChecker.isFtpAvailable(any(LocalTime.class))).willReturn(false);

        task.run();

        verify(repo, never()).findByStatus(eq(Created));
    }

    @Test
    public void should_skip_letter_if_folder_for_its_service_is_not_configured() {
        reset(serviceFolderMapping);

        givenDbContains(
            letterForService("service_A"),
            letterForService("service_B"),
            letterForService("service_C")
        );

        given(serviceFolderMapping.getFolderFor(eq("service_A"))).willReturn(Optional.of("folder_A"));
        given(serviceFolderMapping.getFolderFor(eq("service_B"))).willReturn(Optional.empty());
        given(serviceFolderMapping.getFolderFor(eq("service_C"))).willReturn(Optional.of("folder_C"));

        // when
        task.run();

        // then
        verify(ftpClient).upload(any(), eq(false), eq("folder_A"));
        verify(ftpClient).upload(any(), eq(false), eq("folder_C"));
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
