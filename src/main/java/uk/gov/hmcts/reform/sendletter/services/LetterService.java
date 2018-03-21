package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.data.LetterRepository;
import uk.gov.hmcts.reform.sendletter.data.model.DbLetter;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.logging.AppInsights;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;
import uk.gov.hmcts.reform.sendletter.model.in.LetterPrintedAtPatch;
import uk.gov.hmcts.reform.sendletter.model.in.LetterSentToPrintAtPatch;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;

import java.time.Instant;
import java.util.UUID;

import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final LetterRepository letterRepository;

    public LetterService(LetterRepository letterRepository) {
        this.letterRepository = letterRepository;
    }

    public LetterStatus getStatus(UUID id, String serviceName) {
        return letterRepository
            .getLetterStatus(id, serviceName)
            .orElseThrow(() -> new LetterNotFoundException(id));
    }

    @Transactional
    public UUID send(Letter letter, String serviceName) throws JsonProcessingException {
        Asserts.notEmpty(serviceName, "serviceName");

        final String messageId = generateChecksum(letter);
        final UUID id = UUID.randomUUID();

        log.info("Generated message: id = {} for letter with print queue id = {} and letter id = {} ",
            messageId,
            letter.type,
            id);

        DbLetter dbLetter = new DbLetter(id, serviceName, letter);

        letterRepository.save(dbLetter, Instant.now(), messageId);

        return id;
    }

    @Transactional
    public void updateSentToPrintAt(UUID id, LetterSentToPrintAtPatch patch) {
        int numberOfUpdatedRows = letterRepository.updateSentToPrintAt(id, patch.sentToPrintAt);
        if (numberOfUpdatedRows == 0) {
            throw new LetterNotFoundException(id);
        }
    }

    @Transactional
    public void updatePrintedAt(UUID id, LetterPrintedAtPatch patch) {
        int numberOfUpdatedRows = letterRepository.updatePrintedAt(id, patch.printedAt);
        if (numberOfUpdatedRows == 0) {
            throw new LetterNotFoundException(id);
        }
    }

    @Transactional
    public void updateIsFailed(UUID id) {
        int numberOfUpdatedRows = letterRepository.updateIsFailed(id);
        if (numberOfUpdatedRows == 0) {
            throw new LetterNotFoundException(id);
        }
    }

    public void checkPrintState() {
        // TODO
        letterRepository.getStaleLetters();
    }
}
