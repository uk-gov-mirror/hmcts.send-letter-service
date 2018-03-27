package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.entity.LetterState;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfCreator;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.duplex.DuplexPreparator;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final PdfCreator pdfCreator = new PdfCreator(new DuplexPreparator());
    private final LetterRepository letterRepository;
    private final ObjectMapper mapper;

    public LetterService(LetterRepository letterRepository, ObjectMapper mapper) {
        this.letterRepository = letterRepository;
        this.mapper = mapper;
    }

    @Transactional
    public UUID send(LetterRequest letter, String serviceName) {
        Asserts.notEmpty(serviceName, "serviceName");

        final String messageId = generateChecksum(letter);

        log.info("Generated message: id = {}", messageId);

        Optional<Letter> duplicateLetter = letterRepository.findByMessageIdAndStateOrderByCreatedAtDesc(
            messageId,
            LetterState.Created
        );

        return duplicateLetter
            .map(l -> {
                UUID id = l.getId();
                log.info("Same message found already created. Returning letter id {} instead", id);
                return id;
            })
            .orElseGet(() -> saveNewLetterAndReturnId(letter, messageId, serviceName));
    }

    private UUID saveNewLetterAndReturnId(LetterRequest letterRequest, String messageId, String serviceName) {
        byte[] pdf = pdfCreator.create(letterRequest);

        Letter letter = new Letter(
            messageId,
            serviceName,
            mapper.valueToTree(letterRequest.additionalData),
            letterRequest.type,
            pdf
        );

        UUID letterId = letterRepository.save(letter).getId();

        log.info("Created new letter {}", letterId);

        return letterId;
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
