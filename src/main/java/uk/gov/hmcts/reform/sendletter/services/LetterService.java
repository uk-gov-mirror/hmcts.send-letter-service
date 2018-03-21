package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfCreator;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final LetterRepository letterRepository;
    private final PdfCreator pdfCreator;
    private final uk.gov.hmcts.reform.sendletter.entity.LetterRepository repo;

    public LetterService(LetterRepository letterRepository,
                         PdfCreator pdfCreator,
                         uk.gov.hmcts.reform.sendletter.entity.LetterRepository repo) {
        this.letterRepository = letterRepository;
        this.pdfCreator = pdfCreator;
        this.repo = repo;
    }

    public UUID send(Letter letter, String serviceName) throws JsonProcessingException {
        Asserts.notEmpty(serviceName, "serviceName");

        final String messageId = generateChecksum(letter);
        final UUID id = UUID.randomUUID();

        log.info("Generated message: id = {} for letter with print queue id = {} and letter id = {} ",
            messageId,
            letter.type,
            id);

        byte[] pdf = pdfCreator.create(letter);
        uk.gov.hmcts.reform.sendletter.entity.Letter dbLetter = new uk.gov.hmcts.reform.sendletter.entity.Letter(
            messageId, serviceName, null, letter.type, pdf);

        repo.save(dbLetter);
        return dbLetter.getId();
    }

    public void checkPrintState() {
        // TODO: does previous implementation
        // do anything with this request?
    }

    public LetterStatus getStatus(UUID id, String serviceName) {
        uk.gov.hmcts.reform.sendletter.entity.Letter l =
            repo.findByIdAndService(id, serviceName)
            .orElseThrow(() -> new LetterNotFoundException(id));

        return new LetterStatus(id, l.messageId, toDateTime(l.createdAt),
            toDateTime(l.sentToPrintAt), toDateTime(l.printedAt), l.isFailed);
    }

    public static ZonedDateTime toDateTime(Timestamp stamp) {
        if (null == stamp) {
            return null;
        }
        return stamp.toInstant().atZone(ZoneId.of("UTC"));
    }
}
