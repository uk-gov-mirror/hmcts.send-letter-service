package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DelayedPrintServiceTest {
    @Mock
    private LetterRepository letterRepository;

    private DelayedPrintService delayedPrintService;

    @BeforeEach
    void setUp() {
        delayedPrintService = new DelayedPrintService(letterRepository);
    }

    @Test
    void should_return_delayed_print_file() throws IOException {
        List<Letter> letters = Arrays.asList(createLetter(), createLetter(), createLetter());
        Stream<Letter> stream = letters.stream();
        //given
        given(letterRepository.findDeplayedPostedLetter(isA(LocalDateTime.class),
                isA(LocalDateTime.class), anyInt())).willReturn(stream);


        LocalDateTime current = LocalDateTime.now();
        File deplayLettersAttachment = delayedPrintService.getDeplayLettersAttachment(
                current.minusDays(6), current, 48);
        assertThat(deplayLettersAttachment).isNotEmpty();
        verify(letterRepository).findDeplayedPostedLetter(isA(LocalDateTime.class),
                isA(LocalDateTime.class), anyInt());
    }

    private Letter createLetter() {
        LocalDateTime current = LocalDateTime.now();
        Letter result = mock(Letter.class);
        when(result.getType()).thenReturn("type-1");
        when(result.getService()).thenReturn("testService");
        when(result.getCreatedAt()).thenReturn(current);
        when(result.getId()).thenReturn(UUID.randomUUID());
        when(result.isEncrypted()).thenReturn(true);
        when(result.getSentToPrintAt()).thenReturn(current.plusMinutes(10));
        when(result.getPrintedAt()).thenReturn(current.plusDays(3));
        return result;
    }

}