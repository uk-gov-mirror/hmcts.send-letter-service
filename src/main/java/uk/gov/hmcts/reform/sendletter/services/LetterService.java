package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.NotImplementedException;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.FileNameHelper;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfCreator;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.PdfDoc;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.duplex.DuplexPreparator;

import java.sql.Timestamp;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final PdfCreator pdfCreator = new PdfCreator(new DuplexPreparator());
    private final LetterRepository letterRepository;
    private final Zipper zipper;
    private final ObjectMapper mapper;

    public LetterService(LetterRepository letterRepository, Zipper zipper, ObjectMapper mapper) {
        this.letterRepository = letterRepository;
        this.zipper = zipper;
        this.mapper = mapper;
    }

    @Transactional
    public UUID send(LetterRequest letter, String serviceName) {
        Asserts.notEmpty(serviceName, "serviceName");

        final String messageId = generateChecksum(letter);

        log.info("Generated message: id = {}", messageId);

        Optional<Letter> duplicateLetter = letterRepository.findByMessageIdAndStatusOrderByCreatedAtDesc(
            messageId,
            Created
        );

        return duplicateLetter
            .map(l -> {
                UUID id = l.getId();
                log.info("Same message found already created. Returning letter id {} instead", id);
                return id;
            })
            .orElseGet(() -> saveNewLetterAndReturnId(letter, messageId, serviceName));
    }

    @Transactional
    public UUID send(LetterWithPdfsRequest letter, String serviceName) {
        throw new NotImplementedException();
    }

    private UUID saveNewLetterAndReturnId(LetterRequest letterRequest, String messageId, String serviceName) {
        UUID id = UUID.randomUUID();

        byte[] zipContent = zipper.zip(
            new PdfDoc(
                FileNameHelper.generateName(letterRequest.type, serviceName, id, "pdf"),
                pdfCreator.create(letterRequest)
            )
        );

        // TODO: encrypt zip content

        Letter letter = new Letter(
            id,
            messageId,
            serviceName,
            mapper.valueToTree(letterRequest.additionalData),
            letterRequest.type,
            zipContent
        );

        letterRepository.save(letter);

        log.info("Created new letter {}", id);

        return id;
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
