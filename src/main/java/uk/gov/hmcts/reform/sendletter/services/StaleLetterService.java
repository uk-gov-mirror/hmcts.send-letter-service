package uk.gov.hmcts.reform.sendletter.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.EventType;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterEvent;
import uk.gov.hmcts.reform.sendletter.entity.LetterEventRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotStaleException;
import uk.gov.hmcts.reform.sendletter.services.date.DateCalculator;
import uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask;
import uk.gov.hmcts.reform.sendletter.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static java.time.temporal.ChronoUnit.DAYS;
import static uk.gov.hmcts.reform.sendletter.entity.EventType.MANUALLY_MARKED_AS_CREATED;
import static uk.gov.hmcts.reform.sendletter.entity.EventType.MANUALLY_MARKED_AS_NOT_SENT;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Uploaded;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.EUROPE_LONDON;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.UTC;

/**
 * Service to handle stale letters.
 */
@Service
public class StaleLetterService {
    private static final Logger log = LoggerFactory.getLogger(StaleLetterService.class);
    private static final ZoneId SERVICE_TIME_ZONE_ID = ZoneId.of(EUROPE_LONDON);
    private static final ZoneId DB_TIME_ZONE_ID = ZoneId.of(UTC);

    private final DateCalculator dateCalculator;
    private final LetterRepository letterRepository;
    private final LetterEventRepository letterEventRepository;
    private final int minStaleLetterAgeInBusinessDays;
    private final LocalTime ftpDowntimeStart;
    private final Clock clock;

    public static final  List<LetterStatus> LETTER_STATUS_TO_IGNORE =
            List.of(LetterStatus.Posted, LetterStatus.Aborted, LetterStatus.NotSent);

    /**
     * Constructor for the StaleLetterService.
     *
     * @param dateCalculator The date calculator
     * @param letterRepository The repository for letter
     * @param letterEventRepository The repository for letter event
     * @param minStaleLetterAgeInBusinessDays The minimum stale letter age in business days
     * @param ftpDowntimeStart The FTP downtime start
     * @param clock The clock
     */
    public StaleLetterService(
        DateCalculator dateCalculator,
        LetterRepository letterRepository,
        LetterEventRepository letterEventRepository,
        @Value("${stale-letters.min-age-in-business-days}") int minStaleLetterAgeInBusinessDays,
        @Value("${ftp.downtime.from}") String ftpDowntimeStart,
        Clock clock
    ) {
        this.dateCalculator = dateCalculator;
        this.letterRepository = letterRepository;
        this.letterEventRepository = letterEventRepository;
        this.minStaleLetterAgeInBusinessDays = minStaleLetterAgeInBusinessDays;
        this.ftpDowntimeStart = LocalTime.parse(ftpDowntimeStart);
        this.clock = clock;
    }

    /**
     * Get stale letters.
     *
     * @return The stale letters
     */
    public List<BasicLetterInfo> getStaleLetters() {
        LocalDateTime localDateTime = calculateCutOffCreationDate()
                .withZoneSameInstant(DB_TIME_ZONE_ID)
                .toLocalDateTime();
        log.info("Stale letters before {} ", localDateTime);
        return letterRepository.findStaleLetters(localDateTime);
    }

    /**
     * Get stale letters for a specific status and before date.
     *
     * @return The stale letters
     */
    public List<BasicLetterInfo> getStaleLetters(Collection<LetterStatus> statuses, LocalDateTime beforeDateTime) {
        log.info("Stale letters before {} where status in: {}", beforeDateTime, statuses);
        return letterRepository.findByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(statuses, beforeDateTime);
    }

    /**
     * Get stale letters for a specific status and before date that have a non-null sent_to_print_date.
     *
     * @return The stale letters
     */
    public List<BasicLetterInfo> getStaleLettersWithValidPrintDate(
        Collection<LetterStatus> statuses,
        LocalDateTime beforeDateTime) {
        log.info("Stale letters before {} where status in: {}, and sent_to_print_at is not noll",
            beforeDateTime, statuses);
        return letterRepository.findByStatusInAndCreatedAtBeforeAndSentToPrintAtNotNullOrderByCreatedAtAsc(
            statuses, beforeDateTime);
    }

    /**
     * Get weekly stale letters.
     *
     * @return The weekly stale letters
     * @throws IOException If an I/O error occurs
     */
    @Transactional
    public File getWeeklyStaleLetters() throws IOException {
        LocalDateTime localDateTime = calculateCutOffCreationDate()
                .withZoneSameInstant(DB_TIME_ZONE_ID)
                .toLocalDateTime();
        log.info("Stale letters before {} ", localDateTime);
        try (Stream<BasicLetterInfo> weeklyStaleLetters =
                letterRepository.findByStatusNotInAndTypeNotAndCreatedAtBetweenOrderByCreatedAtAsc(
                        LETTER_STATUS_TO_IGNORE, UploadLettersTask.SMOKE_TEST_LETTER_TYPE,
                        localDateTime.minusDays(6), localDateTime)) {
            return CsvWriter.writeStaleLettersReport(weeklyStaleLetters);
        }
    }

    /**
     * Mark stale letter as not sent.
     *
     * @param id The id of the letter
     * @return The number of letters marked as not sent
     */
    @Transactional
    public int markStaleLetterAsNotSent(UUID id) {
        log.info("Marking stale letter as not sent {} as being stale", id);

        prepareChangingLetterStatus(
                id,
                MANUALLY_MARKED_AS_NOT_SENT,
                "Letter marked manually as not sent as being stale"
        );

        return letterRepository.markStaleLetterAsNotSent(id);
    }

    /**
     * Mark stale letter as created.
     *
     * @param id The id of the letter
     * @return The number of letters marked as created
     */
    @Transactional
    public int markStaleLetterAsCreated(UUID id) {
        log.info("Marking the letter id {} as created to re-upload to FTP server", id);

        prepareChangingLetterStatus(
                id,
                MANUALLY_MARKED_AS_CREATED,
                "Letter marked manually as created for reprocessing"
        );

        return letterRepository.markLetterAsCreated(id);
    }

    /**
     * Prepare changing letter status.
     *
     * @param id The id of the letter
     * @param manuallyMarkedStatus The manually marked status
     * @param notes The notes
     */
    private void prepareChangingLetterStatus(UUID id, EventType manuallyMarkedStatus, String notes) {
        Optional<Letter> letterOpt = letterRepository.findById(id);

        if (letterOpt.isEmpty()) {
            throw new LetterNotFoundException(id);
        }

        Letter letter = letterOpt.get();

        checkIfLetterIsStale(letter);

        createLetterEvent(
                letter,
                manuallyMarkedStatus,
                notes
        );
    }

    /**
     * Check if letter is stale.
     *
     * @param letter The letter
     */
    private void checkIfLetterIsStale(Letter letter) {
        LocalDateTime localDateTime = calculateCutOffCreationDate()
                .withZoneSameInstant(DB_TIME_ZONE_ID)
                .toLocalDateTime();
        if (letter.getStatus() != Uploaded || !letter.getCreatedAt().isBefore(localDateTime)) {
            throw new LetterNotStaleException(letter.getId());
        }
    }

    /**
     * Create letter event.
     *
     * @param letter The letter
     * @param type The type
     * @param notes The notes
     */
    private void createLetterEvent(Letter letter, EventType type, String notes) {
        log.info("Creating letter event {} for letter {}, notes {}", type, letter.getId(), notes);

        LetterEvent letterEvent = new LetterEvent(letter, type, notes, Instant.now());

        letterEventRepository.save(letterEvent);
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

    /**
     * Get download file.
     *
     * @return The download file
     * @throws IOException If an I/O error occurs
     */
    public File getDownloadFile() throws IOException {
        List<BasicLetterInfo> staleLetters = getStaleLetters();
        return CsvWriter.writeStaleLettersToCsv(staleLetters);
    }
}
