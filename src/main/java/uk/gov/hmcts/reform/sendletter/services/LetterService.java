package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.NotImplementedException;
import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pdf.generator.HTMLToPDFConverter;
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
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final PdfCreator pdfCreator = new PdfCreator(new DuplexPreparator(), new HTMLToPDFConverter()::convert);
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
        return send(
            new LetterInfo(generateChecksum(letter), serviceName, letter.type, letter.additionalData),
            () -> pdfCreator.createFromTemplates(letter.documents)
        );
    }

    @Transactional
    public UUID send(LetterWithPdfsRequest letter, String serviceName) {
        throw new NotImplementedException();
    }

    private UUID send(LetterInfo letterInfo, Supplier<byte[]> pdfContentSupplier) {
        Asserts.notEmpty(letterInfo.serviceName, "serviceName");

        return letterRepository
            .findByMessageIdAndStatusOrderByCreatedAtDesc(letterInfo.messageId, Created)
            .map(duplicate -> {
                UUID id = duplicate.getId();
                log.info("Same message found already created. Returning letter id {} instead", id);
                return id;
            })
            .orElseGet(() -> saveNewLetter(letterInfo, pdfContentSupplier.get()));
    }

    private UUID saveNewLetter(LetterInfo letterInfo, byte[] pdfContent) {
        UUID id = UUID.randomUUID();

        byte[] zipContent = zipper.zip(
            new PdfDoc(
                FileNameHelper.generateName(letterInfo.type, letterInfo.serviceName, id, "pdf"),
                pdfContent
            )
        );

        // TODO: encrypt zip content

        Letter letter = new Letter(
            id,
            letterInfo.messageId,
            letterInfo.serviceName,
            mapper.valueToTree(letterInfo.additionalData),
            letterInfo.type,
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

    static class LetterInfo {
        public final String messageId;
        public final String serviceName;
        public final String type;
        public final Map<String, Object> additionalData;

        public LetterInfo(String messageId, String serviceName, String type, Map<String, Object> additionalData) {
            this.messageId = messageId;
            this.serviceName = serviceName;
            this.type = type;
            this.additionalData = additionalData;
        }
    }
}
