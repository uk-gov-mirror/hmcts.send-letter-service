package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.model.out.StaleLetter;
import uk.gov.hmcts.reform.sendletter.services.date.DateCalculator;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;

@ExtendWith(MockitoExtension.class)
public class StaleLetterServiceTest {

    @Mock
    private DateCalculator dateCalculator;

    @Mock
    private LetterRepository letterRepository;

    @Mock
    private Clock clock;

    @BeforeEach
    public void setUp() {
        given(letterRepository.findStaleLetters(any())).willReturn(Stream.of());
        given(dateCalculator.subtractBusinessDays(any(), anyInt())).willReturn(ZonedDateTime.now());

        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(EUROPE_LONDON));
        setCurrentTimeAndZone(EUROPE_LONDON, now);
    }

    @Test
    public void getStaleLetters_should_call_date_calculator_with_current_time_when_before_ftp_downtime_start() {
        // given
        String ftpDowntimeStartTime = "16:00";
        int minStaleLetterAgeInBusinessDays = 123;

        ZonedDateTime now = LocalDate.parse("2019-05-03")
            .atTime(15, 59) // currently it's before FTP downtime
            .atZone((ZoneId.of(EUROPE_LONDON)));

        setCurrentTimeAndZone(EUROPE_LONDON, now);

        // when
        staleLetterService(ftpDowntimeStartTime, minStaleLetterAgeInBusinessDays).getStaleLetters();

        // then
        verify(dateCalculator).subtractBusinessDays(now, minStaleLetterAgeInBusinessDays);
    }

    @Test
    public void getStaleLetters_should_call_date_calculator_with_ftp_downtime_start_time_when_after() {
        // given
        String ftpDowntimeStartTime = "16:00";
        int minStaleLetterAgeInBusinessDays = 123;

        ZonedDateTime now = LocalDate.parse("2019-05-03")
            .atTime(16, 1) // currently it's after FTP downtime start time
            .atZone((ZoneId.of(EUROPE_LONDON)));

        setCurrentTimeAndZone(EUROPE_LONDON, now);

        // when
        staleLetterService(ftpDowntimeStartTime, minStaleLetterAgeInBusinessDays).getStaleLetters();

        // then
        ZonedDateTime ftpDowntimeStartToday = now
            .truncatedTo(DAYS)
            .plusNanos(LocalTime.parse(ftpDowntimeStartTime).toNanoOfDay());

        verify(dateCalculator).subtractBusinessDays(
            ftpDowntimeStartToday,
            minStaleLetterAgeInBusinessDays
        );
    }

    @Test
    public void getStaleLetters_should_return_call_the_repository_with_calculated_cut_off_date() {
        // given
        ZonedDateTime cutOffTime = LocalDateTime.parse("2019-05-01T15:34:56.123").atZone(ZoneId.of(EUROPE_LONDON));

        given(dateCalculator.subtractBusinessDays(any(), anyInt())).willReturn(cutOffTime);

        // when
        staleLetterService("16:00", 2).getStaleLetters();

        // then
        verify(letterRepository).findStaleLetters(
            // as the DB stores UTC-based local datetimes, we expect conversion to happen
            cutOffTime.withZoneSameInstant(UTC).toLocalDateTime()
        );
    }

    @Test
    public void getStaleLetters_should_return_all_letters_returned_by_repository() {
        reset(letterRepository);

        List<Letter> letters = Arrays.asList(
            letter("service1", LetterStatus.Created, true),
            letter("service2", LetterStatus.Uploaded, false)
        );

        given(letterRepository.findStaleLetters(any())).willReturn(letters.stream());

        // when
        Stream<StaleLetter> staleLetters =
            staleLetterService("16:00", 2).getStaleLetters();

        // then
        Stream<StaleLetter> expectedStaleLetters = letters.stream().map(letter ->
            new StaleLetter(
                letter.getId(),
                letter.getStatus().name(),
                letter.getService(),
                letter.getCreatedAt(),
                letter.getSentToPrintAt(),
                letter.isFailed()
            )
        );

        assertThat(staleLetters.collect(toList()))
            .usingFieldByFieldElementComparator()
            .isEqualTo(expectedStaleLetters.collect(toList()));
    }

    private StaleLetterService staleLetterService(String ftpDowntimeStart, int minStaleLetterAgeInBusinessDays) {
        return new StaleLetterService(
            dateCalculator,
            letterRepository,
            minStaleLetterAgeInBusinessDays,
            ftpDowntimeStart,
            clock
        );
    }

    private void setCurrentTimeAndZone(String zone, ZonedDateTime dateTime) {
        ZoneId zoneId = ZoneId.of(zone);
        given(clock.instant()).willReturn(dateTime.toInstant());
        given(clock.getZone()).willReturn(zoneId);
    }

    private Letter letter(String service, LetterStatus status, Boolean isFailed) {
        Letter letter = mock(Letter.class);
        LocalDateTime sentToPrintAt = LocalDateTime.now().minusMinutes(ThreadLocalRandom.current().nextInt(100));
        LocalDateTime createdAt = sentToPrintAt.minusMinutes(ThreadLocalRandom.current().nextInt(100));

        given(letter.getId()).willReturn(UUID.randomUUID());
        given(letter.getSentToPrintAt()).willReturn(sentToPrintAt);
        given(letter.getCreatedAt()).willReturn(createdAt);
        given(letter.getService()).willReturn(service);
        given(letter.getStatus()).willReturn(status);
        given(letter.isFailed()).willReturn(isFailed);

        return letter;
    }
}
