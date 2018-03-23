package uk.gov.hmcts.reform.sendletter.services;

import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfCreator;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.duplex.DuplexPreparator;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final PdfCreator pdfCreator = new PdfCreator(new DuplexPreparator());
    private final LetterRepository letterRepository;

    public LetterService(LetterRepository letterRepository) {
        this.letterRepository = letterRepository;
    }

    public UUID send(LetterRequest letter, String serviceName) {
        Asserts.notEmpty(serviceName, "serviceName");

        final String messageId = generateChecksum(letter);

        log.info("Generated message: id = {} for letter with print queue id = {}", messageId, letter.type);

        byte[] pdf = pdfCreator.create(letter);
        Letter dbLetter = new Letter(messageId, serviceName, null, letter.type, pdf);

        letterRepository.save(dbLetter);
        return dbLetter.getId();
    }

    public void checkPrintState() {
        // TODO: does previous implementation
        // do anything with this request?
    }

    public LetterStatus getStatus(UUID id, String serviceName) {
        Letter letter = letterRepository
            .findByIdAndService(id, serviceName)
            .orElseThrow(() -> new LetterNotFoundException(id));

        return new LetterStatus(
            id,
            letter.getMessageId(),
            toDateTime(letter.getCreatedAt()),
            toDateTime(letter.getSentToPrintAt()),
            toDateTime(letter.getPrintedAt()),
            letter.isFailed()
        );
    }

    public static ZonedDateTime toDateTime(Timestamp stamp) {
        if (null == stamp) {
            return null;
        }
        return stamp.toInstant().atZone(ZoneId.of("UTC"));
    }
}
