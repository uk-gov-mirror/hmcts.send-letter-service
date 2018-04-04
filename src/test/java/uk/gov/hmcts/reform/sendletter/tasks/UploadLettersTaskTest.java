package uk.gov.hmcts.reform.sendletter.tasks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.services.FtpAvailabilityChecker;
import uk.gov.hmcts.reform.sendletter.services.FtpClient;

import java.time.LocalTime;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask.SMOKE_TEST_LETTER_TYPE;

@RunWith(MockitoJUnitRunner.class)
public class UploadLettersTaskTest {

    @Mock private LetterRepository repo;
    @Mock private FtpClient ftpClient;
    @Mock private FtpAvailabilityChecker availabilityChecker;

    private UploadLettersTask task;

    @Before
    public void setUp() throws Exception {
        given(availabilityChecker.isFtpAvailable(any(LocalTime.class))).willReturn(true);
        this.task = new UploadLettersTask(repo, ftpClient, availabilityChecker);
    }

    @After
    public void tearDown() {
        reset(availabilityChecker, repo);
    }

    @Test
    public void should_handle_smoke_test_letters() throws Exception {

        givenDbContains(letterOfType(SMOKE_TEST_LETTER_TYPE));
        task.run();
        verify(ftpClient).upload(any(), eq(true));

        givenDbContains(letterOfType("not_" + SMOKE_TEST_LETTER_TYPE));
        task.run();
        verify(ftpClient).upload(any(), eq(false));
    }

    @Test
    public void should_not_start_process_if_ftp_is_not_available() {
        reset(availabilityChecker);
        given(availabilityChecker.isFtpAvailable(any(LocalTime.class))).willReturn(false);

        task.run();

        verify(repo, never()).findByStatus(Created);
    }

    private Letter letterOfType(String type) {
        return new Letter(UUID.randomUUID(), "msgId", "cmc", null, type, "hello".getBytes());
    }

    private void givenDbContains(Letter letter) {
        given(repo.findByStatus(Created))
            .willReturn(Stream.of(letter));
    }

}
