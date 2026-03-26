package uk.gov.hmcts.reform.sendletter.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.EventType;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterEvent;
import uk.gov.hmcts.reform.sendletter.entity.LetterEventRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.exception.UnableToMarkLetterPostedException;
import uk.gov.hmcts.reform.sendletter.exception.UnableToMarkLetterPostedLocallyException;
import uk.gov.hmcts.reform.sendletter.exception.UnableToReprocessLetterException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.sendletter.entity.EventType.MANUALLY_MARKED_AS_ABORTED;
import static uk.gov.hmcts.reform.sendletter.entity.EventType.MANUALLY_MARKED_AS_CREATED;
import static uk.gov.hmcts.reform.sendletter.entity.EventType.MANUALLY_MARKED_AS_POSTED;
import static uk.gov.hmcts.reform.sendletter.entity.EventType.MANUALLY_MARKED_AS_POSTED_LOCALLY;
import static uk.gov.hmcts.reform.sendletter.entity.EventType.MARKED_AS_NO_REPORT_ABORTED;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.FailedToUpload;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Posted;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Uploaded;

/**
 * Service for letter actions.
 */
@Service
public class LetterActionService {
    private static final Logger log = LoggerFactory.getLogger(StaleLetterService.class);

    private final LetterRepository letterRepository;
    private final LetterEventRepository letterEventRepository;
    private final StaleLetterService staleLetterService;

    /**
     * Constructor for the LetterActionService.
     * @param letterRepository The repository for letter
     * @param letterEventRepository The repository for letter event
     * @param staleLetterService The service for stale letter
     */
    public LetterActionService(LetterRepository letterRepository,
                               LetterEventRepository letterEventRepository,
                               StaleLetterService staleLetterService) {
        this.letterRepository = letterRepository;
        this.letterEventRepository = letterEventRepository;
        this.staleLetterService = staleLetterService;
    }

    /**
     * Mark a letter as aborted.
     * @param id The id of the letter to mark as aborted
     * @return The number of letters marked as aborted
     */
    @Transactional
    public int markLetterAsAborted(UUID id) {
        log.info("Marking the letter id {} as Aborted", id);

        Optional<Letter> letterOpt = letterRepository.findById(id);

        if (letterOpt.isEmpty()) {
            throw new LetterNotFoundException(id);
        }

        createLetterEvent(
            letterOpt.get(),
            MANUALLY_MARKED_AS_ABORTED,
            "Letter marked manually as Aborted to stop processing");

        return letterRepository.markLetterAsAborted(id);
    }

    /**
     * Mark a letter as aborted due to a missing report.
     * @param id The id of the letter to mark as NoReportAborted
     * @return The number of letters marked as NoReportAborted
     */
    @Transactional
    public int markLetterAsNoReportAborted(UUID id) {
        log.info("Marking the letter id {} as NoReportAborted", id);

        Optional<Letter> letterOpt = letterRepository.findById(id);

        if (letterOpt.isEmpty()) {
            throw new LetterNotFoundException(id);
        }

        createLetterEvent(
            letterOpt.get(),
            MARKED_AS_NO_REPORT_ABORTED,
            "Letter marked NoReportAborted to stop processing");

        return letterRepository.markLetterAsNoReportAborted(id);
    }

    /**
     * Mark a letter as created to re-upload to SFTP server.
     * @param id The id of the letter to mark as created
     * @return The number of letters marked as created
     */
    @Transactional
    public int markLetterAsCreated(UUID id) {
        log.info("Marking the letter id {} as Created to re-upload to SFTP server", id);

        Optional<Letter> letterOpt = letterRepository.findById(id);

        if (letterOpt.isEmpty()) {
            throw new LetterNotFoundException(id);
        }

        Letter letter = letterOpt.get();
        checkLetterStatusForLetterReUpload(letter);

        if (letter.getStatus() == FailedToUpload) {
            createLetterEvent(letter, MANUALLY_MARKED_AS_CREATED, "Letter marked manually as Created to re-process");
            return letterRepository.markLetterAsCreated(letter.getId());
        } else {
            return staleLetterService.markStaleLetterAsCreated(id);
        }
    }

    /**
     * Mark a letter as posted locally.
     * @param id The id of the letter to mark as posted locally
     * @return The number of letters marked as posted locally
     */
    @Transactional
    public int markLetterAsPostedLocally(UUID id) {
        log.info("Marking the letter id {} as PostedLocally", id);

        Optional<Letter> letterOpt = letterRepository.findById(id);

        if (letterOpt.isEmpty()) {
            throw new LetterNotFoundException(id);
        }

        Letter letter = letterOpt.get();
        checkLetterStatusForMarkPostedLocally(letter);

        createLetterEvent(
            letterOpt.get(),
            MANUALLY_MARKED_AS_POSTED_LOCALLY,
            "Letter marked manually as PostedLocally as the letter was printed and posted by CTSC");

        return letterRepository.markLetterAsPostedLocally(id);
    }

    /**
     * Mark a letter as posted.
     * @param id The id of the letter to mark as posted
     * @param printedOn The date the letter was printed
     * @param printedAt The time the letter was printed
     * @return The number of letters marked as posted
     */
    @Transactional
    public int markLetterAsPosted(UUID id,
                                  LocalDate printedOn,
                                  LocalTime printedAt) {
        log.info("Marking the letter id {} as Posted", id);

        Optional<Letter> letterOpt = letterRepository.findById(id);
        if (letterOpt.isEmpty()) {
            throw new LetterNotFoundException(id);
        }

        Letter letter = letterOpt.get();
        checkLetterStatusForMarkPosted(letter);

        final ZonedDateTime printedDateTime = ZonedDateTime.of(printedOn, printedAt, ZoneOffset.UTC);

        createLetterEvent(
            letterOpt.get(),
            MANUALLY_MARKED_AS_POSTED,
            "Letter marked manually as Posted");

        return letterRepository.markLetterAsPosted(id, printedDateTime.toLocalDateTime());
    }

    /**
     * Check if a letter can be re-processed.
     * @param letter The letter to check
     */
    private void checkLetterStatusForLetterReUpload(Letter letter) {
        if (!List.of(FailedToUpload, Uploaded).contains(letter.getStatus())) {
            throw new UnableToReprocessLetterException(
                String.format("Letter with ID '%s', status '%s' can not be re-processed",
                    letter.getId(), letter.getStatus()));
        }

    }

    /**
     * Check if a letter can be marked as posted locally.
     * @param letter The letter to check
     */
    private void checkLetterStatusForMarkPostedLocally(Letter letter) {
        if (!List.of(Uploaded, Posted).contains(letter.getStatus())) {
            throw new UnableToMarkLetterPostedLocallyException(
                String.format("Letter with ID '%s', status '%s' can not be marked as %s",
                    letter.getId(), letter.getStatus(), LetterStatus.PostedLocally));
        }

    }

    /**
     * Check if a letter can be marked as posted.
     * @param letter The letter to check
     */
    private void checkLetterStatusForMarkPosted(Letter letter) {
        if (letter.getStatus() == Posted) {
            throw new UnableToMarkLetterPostedException(
                String.format("Letter with ID '%s', status '%s' can not be marked as %s",
                    letter.getId(), letter.getStatus(), LetterStatus.Posted));
        }

    }

    /**
     * Create a letter event.
     * @param letter The letter
     * @param type The event type
     * @param notes The notes
     */
    private void createLetterEvent(Letter letter, EventType type, String notes) {
        log.info("Creating letter event {} for letter {}, notes {}", type, letter.getId(), notes);

        LetterEvent letterEvent = new LetterEvent(letter, type, notes, Instant.now());

        letterEventRepository.save(letterEvent);
    }
}
