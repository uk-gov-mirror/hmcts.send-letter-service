package uk.gov.hmcts.reform.sendletter.tasks;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.LetterDataAccessService;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

import static java.time.ZoneOffset.UTC;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClearOldLetterContentTaskTest {

    @Mock LetterDataAccessService letterService;
    @Mock Clock clock;

    @Test
    void should_call_repo_to_clear_uploaded_letters() {
        // given
        var now = LocalDateTime.now();
        given(clock.instant()).willReturn(now.toInstant(UTC));
        given(clock.getZone()).willReturn(UTC);
        var ttl = Duration.ofDays(30);

        var task = new ClearOldLetterContentTask(letterService, ttl, clock);

        // when
        task.run();

        // then
        verify(letterService).clearFileContent(
            now.minusDays(30),
            LetterStatus.Uploaded
        );
    }
}
