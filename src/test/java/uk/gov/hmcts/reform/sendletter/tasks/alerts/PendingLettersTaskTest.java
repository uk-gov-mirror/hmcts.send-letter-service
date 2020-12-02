package uk.gov.hmcts.reform.sendletter.tasks.alerts;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.services.PendingLettersService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingLettersTaskTest {

    @Mock
    private AppInsights appInsights;

    @Mock
    private PendingLettersService pendingLettersService;

    @Captor
    ArgumentCaptor<BasicLetterInfo> basicLetterCaptor;

    private PendingLettersTask task;
    private int lettersBeforeMinutes = 5;

    @BeforeEach
    void setUp() {
        task = new PendingLettersTask(pendingLettersService, appInsights, lettersBeforeMinutes);
    }

    @Test
    void should_not_invoke_appInsight() {
        when(pendingLettersService.getPendingLettersCreatedBeforeTime(lettersBeforeMinutes))
                .thenReturn(Stream.empty());

        task.run();

        verify(appInsights, never()).trackPendingLetter(any());
    }

    @Test
    void should_invoke_appInsight() {
        List<BasicLetterInfo> basicLetterInfos = Arrays.asList(createBasicLetterInfo(), createBasicLetterInfo());
        when(pendingLettersService.getPendingLettersCreatedBeforeTime(lettersBeforeMinutes))
                .thenReturn(basicLetterInfos.stream());

        task.run();

        verify(appInsights, times(2)).trackPendingLetter(basicLetterCaptor.capture());
        List<BasicLetterInfo> allValues = basicLetterCaptor.getAllValues();

        assertThat(allValues).extracting("id")
                .containsExactly(basicLetterInfos.get(0).getId(), basicLetterInfos.get(1).getId());
    }

    private BasicLetterInfo createBasicLetterInfo() {
        BasicLetterInfo basicLetterInfo = mock(BasicLetterInfo.class);
        UUID uuid = UUID.randomUUID();
        basicLetterInfo.setId(uuid);
        return basicLetterInfo;
    }
}