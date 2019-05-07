package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.model.out.StaleLetter;
import uk.gov.hmcts.reform.sendletter.services.date.DateCalculator;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.UTC;

@Service
public class StaleLetterService {

    private static final ZoneId SERVICE_TIME_ZONE_ID = ZoneId.of(EUROPE_LONDON);
    private static final ZoneId DB_TIME_ZONE_ID = ZoneId.of(UTC);

    private final DateCalculator dateCalculator;
    private final LetterRepository letterRepository;
    private final int minStaleLetterAgeInBusinessDays;
    private final LocalTime ftpDowntimeStart;
    private final Clock clock;

    public StaleLetterService(
        DateCalculator dateCalculator,
        LetterRepository letterRepository,
        @Value("${stale-letters.min-age-in-business-days}") int minStaleLetterAgeInBusinessDays,
        @Value("${ftp.downtime.from}") String ftpDowntimeStart,
        Clock clock
    ) {
        this.dateCalculator = dateCalculator;
        this.letterRepository = letterRepository;
        this.minStaleLetterAgeInBusinessDays = minStaleLetterAgeInBusinessDays;
        this.ftpDowntimeStart = LocalTime.parse(ftpDowntimeStart);
        this.clock = clock;
    }

    public Stream<StaleLetter> getStaleLetters() {
        Stream<Letter> dbLetters = letterRepository.findStaleLetters(
            calculateCutOffCreationDate()
                .withZoneSameInstant(DB_TIME_ZONE_ID)
                .toLocalDateTime()
        );

        return dbLetters.map(this::mapToStaleLetter);
    }

    /**
     * Calculates the cut-off creation date for stale letters.
     *
     * @return Cut-off creation date for stale letters. Only letters older than that
     *         can be considered stale.
     */
    private ZonedDateTime calculateCutOffCreationDate() {
        ZonedDateTime now =
            ZonedDateTime
                .now(clock)
                .withZoneSameInstant(SERVICE_TIME_ZONE_ID);

        // If on a given day a letter was created within FTP downtime window or later,
        // it should be treated like it was created the next day.
        LocalTime ftpDowntimeAdjustedTime =
            now.toLocalTime().isBefore(ftpDowntimeStart)
                ? now.toLocalTime()
                : ftpDowntimeStart;

        ZonedDateTime todayWithFtpDowntimeAdjustedTime =
            now
                .truncatedTo(DAYS)
                .plusNanos(ftpDowntimeAdjustedTime.toNanoOfDay());

        return dateCalculator.subtractBusinessDays(
            todayWithFtpDowntimeAdjustedTime,
            minStaleLetterAgeInBusinessDays
        );
    }

    private StaleLetter mapToStaleLetter(Letter dbLetter) {
        return new StaleLetter(
            dbLetter.getId(),
            dbLetter.getStatus().name(),
            dbLetter.getService(),
            dbLetter.getCreatedAt(),
            dbLetter.getSentToPrintAt(),
            dbLetter.isFailed()
        );
    }
}
