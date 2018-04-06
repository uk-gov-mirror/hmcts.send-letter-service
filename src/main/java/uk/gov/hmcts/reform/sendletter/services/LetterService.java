package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pdf.generator.HTMLToPDFConverter;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.exception.UnsupportedLetterRequestTypeException;
import uk.gov.hmcts.reform.sendletter.model.in.ILetterRequest;
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
import java.util.UUID;

import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final PdfCreator pdfCreator;
    private final LetterRepository letterRepository;
    private final Zipper zipper;
    private final ObjectMapper mapper;

    public LetterService(
        PdfCreator pdfCreator,
        LetterRepository letterRepository,
        Zipper zipper,
        ObjectMapper mapper
    ) {
        this.pdfCreator = pdfCreator;
        this.letterRepository = letterRepository;
        this.zipper = zipper;
        this.mapper = mapper;
    }

    // TODO: remove
    @Autowired
    public LetterService(
        LetterRepository letterRepository,
        Zipper zipper,
        ObjectMapper mapper
    ) {
        this(
            new PdfCreator(new DuplexPreparator(), new HTMLToPDFConverter()::convert),
            letterRepository,
            zipper,
            mapper
        );
    }

    @Transactional
    public UUID send(ILetterRequest letter, String serviceName) {
        String messageId = generateChecksum(letter);
        Asserts.notEmpty(serviceName, "serviceName");

        return letterRepository
            .findByMessageIdAndStatusOrderByCreatedAtDesc(messageId, Created)
            .map(duplicate -> {
                UUID id = duplicate.getId();
                log.info("Same message found already created. Returning letter id {} instead", id);
                return id;
            })
            .orElseGet(() -> saveNewLetter(letter, messageId, serviceName));
    }

    private UUID saveNewLetter(ILetterRequest letter, String messageId, String serviceName) {
        UUID id = UUID.randomUUID();

        byte[] zipContent = zipper.zip(
            new PdfDoc(
                FileNameHelper.generateName(letter.getType(), serviceName, id, "pdf"),
                getPdfContent(letter)
            )
        );

        // TODO: encrypt zip content

        Letter dbLetter = new Letter(
            id,
            messageId,
            serviceName,
            mapper.valueToTree(letter.getAdditionalData()),
            letter.getType(),
            zipContent
        );

        letterRepository.save(dbLetter);

        log.info("Created new letter {}", id);

        return id;
    }

    private byte[] getPdfContent(ILetterRequest letter) {
        if (letter instanceof LetterRequest) {
            return pdfCreator.createFromTemplates(((LetterRequest) letter).documents);
        } else if (letter instanceof LetterWithPdfsRequest) {
            return pdfCreator.createFromBase64Pdfs(((LetterWithPdfsRequest) letter).documents);
        } else {
            throw new UnsupportedLetterRequestTypeException();
        }
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
