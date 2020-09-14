package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.util.Asserts;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.exception.ServiceNotConfiguredException;
import uk.gov.hmcts.reform.sendletter.exception.UnsupportedLetterRequestTypeException;
import uk.gov.hmcts.reform.sendletter.model.PdfDoc;
import uk.gov.hmcts.reform.sendletter.model.in.ILetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsAndNumberOfCopiesRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.encryption.PgpEncryptionUtil;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;
import uk.gov.hmcts.reform.sendletter.services.util.FileNameHelper;
import uk.gov.hmcts.reform.sendletter.services.util.FinalPackageFileNameHelper;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static java.time.LocalDateTime.now;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);
    private static final String YES = "yes";

    private final PdfCreator pdfCreator;
    private final LetterRepository letterRepository;
    private final Zipper zipper;
    private final ObjectMapper mapper;
    private final boolean isEncryptionEnabled;
    private final PGPPublicKey pgpPublicKey;
    private final ServiceFolderMapping serviceFolderMapping;


    public LetterService(
        PdfCreator pdfCreator,
        LetterRepository letterRepository,
        Zipper zipper,
        ObjectMapper mapper,
        @Value("${encryption.enabled}") Boolean isEncryptionEnabled,
        @Value("${encryption.publicKey}") String encryptionPublicKey,
        ServiceFolderMapping serviceFolderMapping
    ) {
        this.pdfCreator = pdfCreator;
        this.letterRepository = letterRepository;
        this.zipper = zipper;
        this.mapper = mapper;
        this.isEncryptionEnabled = isEncryptionEnabled;
        this.pgpPublicKey = loadPgpPublicKey(encryptionPublicKey);
        this.serviceFolderMapping = serviceFolderMapping;
    }

    @Transactional
    public UUID save(ILetterRequest letter, String serviceName) {
        String checksum = generateChecksum(letter);
        Asserts.notEmpty(serviceName, "serviceName");

        if (serviceFolderMapping.getFolderFor(serviceName).isEmpty()) {
            throw new ServiceNotConfiguredException("No configuration for service " + serviceName + " found");
        }

        return letterRepository
            .findByChecksumAndStatusOrderByCreatedAtDesc(checksum, Created)
            .map(duplicate -> {
                UUID id = duplicate.getId();
                log.info("Same message found already created. Returning letter id {} instead", id);
                return id;
            })
            .orElseGet(() -> saveNewLetter(letter, checksum, serviceName));
    }

    private UUID saveNewLetter(ILetterRequest letter, String messageId, String serviceName) {
        UUID id = UUID.randomUUID();

        byte[] zipContent = zipper.zip(
            new PdfDoc(
                FileNameHelper.generatePdfName(letter.getType(), serviceName, id),
                getPdfContent(letter)
            )
        );

        LocalDateTime createdAtTime = now();
        Letter dbLetter = new Letter(
            id,
            messageId,
            serviceName,
            mapper.valueToTree(letter.getAdditionalData()),
            letter.getType(),
            isEncryptionEnabled ? encryptZipContents(letter, serviceName, id, zipContent, createdAtTime) : zipContent,
            isEncryptionEnabled,
            getEncryptionKeyFingerprint(),
            createdAtTime,
            getCopies(letter)
        );

        letterRepository.save(dbLetter);

        log.info("Created new letter {} for service {}", id, serviceName);

        return id;
    }

    private byte[] encryptZipContents(
        ILetterRequest letter,
        String serviceName,
        UUID id,
        byte[] zipContent,
        LocalDateTime createdAt
    ) {
        Asserts.notNull(pgpPublicKey, "pgpPublicKey");

        String zipFileName = FinalPackageFileNameHelper.generateName(
            letter.getType(),
            serviceName,
            createdAt,
            id,
            false
        );

        return PgpEncryptionUtil.encryptFile(zipContent, zipFileName, pgpPublicKey);
    }

    private String getEncryptionKeyFingerprint() {
        if (isEncryptionEnabled) {
            return Hex.encodeHexString(this.pgpPublicKey.getFingerprint());
        } else {
            return null;
        }
    }

    private PGPPublicKey loadPgpPublicKey(String encryptionPublicKey) {
        if (!isEncryptionEnabled) {
            log.info("Encryption is not enabled hence not loading the public key");
            return null;
        } else {
            Asserts.notNull(encryptionPublicKey, "encryptionPublicKey");
            return PgpEncryptionUtil.loadPublicKey(encryptionPublicKey.getBytes());
        }
    }

    private byte[] getPdfContent(ILetterRequest letter) {
        if (letter instanceof LetterRequest) {
            return pdfCreator.createFromTemplates(((LetterRequest) letter).documents);
        } else if (letter instanceof LetterWithPdfsRequest) {
            return pdfCreator.createFromBase64Pdfs(((LetterWithPdfsRequest) letter).documents);
        } else if (letter instanceof LetterWithPdfsAndNumberOfCopiesRequest) {
            return pdfCreator
                .createFromBase64PdfWithCopies(
                    ((LetterWithPdfsAndNumberOfCopiesRequest) letter).documents
                );
        } else {
            throw new UnsupportedLetterRequestTypeException();
        }
    }

    private int getCopies(ILetterRequest letter) {
        if (letter instanceof LetterRequest) {
            return ((LetterRequest) letter).documents.size();
        } else if (letter instanceof LetterWithPdfsRequest) {
            return ((LetterWithPdfsRequest) letter).documents.size();
        } else if (letter instanceof LetterWithPdfsAndNumberOfCopiesRequest) {
            return copies.apply((LetterWithPdfsAndNumberOfCopiesRequest) letter);
        } else {
            throw new UnsupportedLetterRequestTypeException();
        }
    }

    private Function<LetterWithPdfsAndNumberOfCopiesRequest, Integer> copies =
        request -> request.documents.stream().mapToInt(doc -> doc.copies).sum();

    public LetterStatus getStatus(UUID id, String isAdditonalDataRequired) {
        Function<JsonNode, Map<String, Object>> additionDataFunction = additionalData -> {
            if (YES.equalsIgnoreCase(isAdditonalDataRequired)) {
                return Optional.ofNullable(additionalData)
                    .map(data -> mapper.convertValue(data, new TypeReference<Map<String, Object>>(){}))
                    .orElse(Collections.emptyMap());
            }
            return null;
        };

        return getStatus(id, additionDataFunction);
    }

    private LetterStatus getStatus(UUID id, Function<JsonNode, Map<String, Object>> additionalDataEvaluator) {
        return letterRepository
            .findById(id)
            .map(letter -> new LetterStatus(
                id,
                letter.getStatus().name(),
                letter.getChecksum(),
                toDateTime(letter.getCreatedAt()),
                toDateTime(letter.getSentToPrintAt()),
                toDateTime(letter.getPrintedAt()),
                additionalDataEvaluator.apply(letter.getAdditionalData()),
                letter.getCopies()
            ))
            .orElseThrow(() -> new LetterNotFoundException(id));
    }

    static ZonedDateTime toDateTime(LocalDateTime dateTime) {
        if (null == dateTime) {
            return null;
        }

        return dateTime.atZone(ZoneId.of("UTC"));
    }
}
