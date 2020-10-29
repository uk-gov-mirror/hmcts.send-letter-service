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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.DuplicateLetter;
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
import java.util.function.ToIntFunction;

import static java.time.LocalDateTime.now;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.services.LetterChecksumGenerator.generateChecksum;

@Service(value = "LetterService")
public class LetterService {

    private static final Logger log = LoggerFactory.getLogger(LetterService.class);

    private final PdfCreator pdfCreator;
    private final LetterRepository letterRepository;
    private final Zipper zipper;
    private final ObjectMapper mapper;
    private final boolean isEncryptionEnabled;
    private final PGPPublicKey pgpPublicKey;
    private final ServiceFolderMapping serviceFolderMapping;
    private final ExecusionService asynService;
    private final DuplicateLetterService duplicateLetterService;

    public LetterService(
            PdfCreator pdfCreator,
            LetterRepository letterRepository,
            Zipper zipper,
            ObjectMapper mapper,
            @Value("${encryption.enabled}") Boolean isEncryptionEnabled,
            @Value("${encryption.publicKey}") String encryptionPublicKey,
            ServiceFolderMapping serviceFolderMapping,
            ExecusionService asynService,
            DuplicateLetterService duplicateLetterService) {
        this.pdfCreator = pdfCreator;
        this.letterRepository = letterRepository;
        this.zipper = zipper;
        this.mapper = mapper;
        this.isEncryptionEnabled = isEncryptionEnabled;
        this.pgpPublicKey = loadPgpPublicKey(encryptionPublicKey);
        this.serviceFolderMapping = serviceFolderMapping;
        this.asynService = asynService;
        this.duplicateLetterService = duplicateLetterService;
    }

    @Transactional
    public UUID save(ILetterRequest letter, String serviceName, String isAsync) {
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
            .orElseGet(() -> saveNewLetter(letter, checksum, serviceName, isAsync));
    }

    private UUID saveNewLetter(ILetterRequest letter, String messageId, String serviceName, String isAsync) {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAtTime = now();
        byte[] zipContent = getFileContent(id, letter, serviceName, createdAtTime);

        if (Boolean.parseBoolean(isAsync)) {
            Runnable logger = () -> log.info("Saving letter id {} in async mode as flag value is {}", id, isAsync);
            asynService.run(() -> saveLetter(letter, messageId, serviceName, id, zipContent, createdAtTime), logger,
                () -> saveDuplicate(letter, id, messageId, serviceName, zipContent, isAsync));
        } else {
            try {
                log.info("Saving letter id {} in sync mode as flag value is {}", id, isAsync);
                asynService.execute(() -> saveLetter(letter, messageId, serviceName, id, zipContent, createdAtTime));
            } catch (DataIntegrityViolationException dataIntegrityViolationException) {
                Runnable logger = () -> log.error("Duplicate record ", dataIntegrityViolationException);
                asynService.run(() -> saveDuplicate(letter, id, messageId, serviceName, zipContent, isAsync), logger,
                    () -> {});
                throw dataIntegrityViolationException;
            }
        }
        log.info("Returning letter id {} for service {}", id, serviceName);

        return id;
    }

    @Transactional
    public void saveLetter(ILetterRequest letter, String messageId, String serviceName, UUID id,
                           byte[] zipContent, LocalDateTime createdAtTime) {

        Letter dbLetter = new Letter(
            id,
            messageId,
            serviceName,
            mapper.valueToTree(letter.getAdditionalData()),
            letter.getType(),
            zipContent,
            isEncryptionEnabled,
            getEncryptionKeyFingerprint(),
            createdAtTime,
            getCopies(letter)
        );

        letterRepository.save(dbLetter);
        log.info("Created new letter record with id {} for service {}", id, serviceName);
    }

    @Transactional
    public void saveDuplicate(ILetterRequest letter, UUID id, String checksum, String serviceName, byte[] zipContent,
                              String isAsync) {
        DuplicateLetter duplicateLetter = getDuplicateLetter(letter, id, checksum, serviceName, zipContent,
                isAsync);
        duplicateLetterService.save(duplicateLetter);
        log.info("Created new duplicate record with id {} for service {}", id, serviceName);
    }

    private DuplicateLetter getDuplicateLetter(ILetterRequest letter, UUID id,
                                               String checksum, String serviceName, byte[] zipContent, String isAsync) {
        LocalDateTime createdAtTime = now();
        return new DuplicateLetter(
                id,
                checksum,
                serviceName,
                mapper.valueToTree(letter.getAdditionalData()),
                letter.getType(),
                isEncryptionEnabled ? encryptZipContents(letter, serviceName, id, zipContent, createdAtTime) :
                        zipContent,
                isEncryptionEnabled,
                getEncryptionKeyFingerprint(),
                createdAtTime,
                getCopies(letter),
                isAsync
        );
    }

    private byte[] getFileContent(UUID id, ILetterRequest letter, String serviceName, LocalDateTime createdAtTime) {
        byte[] zipContent = zipper.zip(
                new PdfDoc(
                        FileNameHelper.generatePdfName(letter.getType(), serviceName, id),
                        getPdfContent(letter)
                )
        );

        if (isEncryptionEnabled) {
            zipContent = encryptZipContents(letter, serviceName, id, zipContent, createdAtTime);
        }
        return zipContent;
    }

    private byte[] encryptZipContents(
        ILetterRequest letter,
        String serviceName,
        UUID id,
        byte[] zipContent,
        LocalDateTime createdAt
    ) {
        Asserts.notNull(pgpPublicKey, "pgpPublicKey");
        log.info("Clear file size {} ", zipContent.length);
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
        int letterCount = -1;
        if (letter instanceof LetterRequest) {
            letterCount = ((LetterRequest) letter).documents.size();
        } else if (letter instanceof LetterWithPdfsRequest) {
            letterCount = ((LetterWithPdfsRequest) letter).documents.size();
        } else if (letter instanceof LetterWithPdfsAndNumberOfCopiesRequest) {
            letterCount = copies.applyAsInt((LetterWithPdfsAndNumberOfCopiesRequest) letter);
        }
        return letterCount;
    }

    private ToIntFunction<LetterWithPdfsAndNumberOfCopiesRequest> copies =
        request -> request.documents.stream().mapToInt(doc -> doc.copies).sum();

    public LetterStatus getStatus(UUID id, String isAdditonalDataRequired, String isDuplicate) {
        log.info("Getting letter status for id {} ", id);

        duplicateCheck(id, isDuplicate);

        Function<JsonNode, Map<String, Object>> additionDataFunction = additionalData -> {
            if (Boolean.parseBoolean(isAdditonalDataRequired)) {
                return Optional.ofNullable(additionalData)
                    .map(data -> mapper.convertValue(data, new TypeReference<Map<String, Object>>(){}))
                    .orElse(Collections.emptyMap());
            }
            return null;
        };

        LetterStatus letterStatus = letterRepository
                .findById(id)
                .map(letter -> new LetterStatus(
                        id,
                        letter.getStatus().name(),
                        letter.getChecksum(),
                        toDateTime(letter.getCreatedAt()),
                        toDateTime(letter.getSentToPrintAt()),
                        toDateTime(letter.getPrintedAt()),
                        additionDataFunction.apply(letter.getAdditionalData()),
                        letter.getCopies()
                ))
                .orElseThrow(() -> new LetterNotFoundException(id));
        log.info("Returning  letter status for letter {} ", letterStatus);
        return letterStatus;
    }

    private void duplicateCheck(UUID id, String isDuplicate) {
        if (Boolean.parseBoolean(isDuplicate)) {
            Optional<DuplicateLetter> optDuplicateLetter = duplicateLetterService.isPresent(id);
            if (optDuplicateLetter.isPresent()) {
                DuplicateLetter duplicateLetter = optDuplicateLetter.get();
                String duplicateMessage = String.join(",",
                        "Duplicate record for service:", duplicateLetter.getService(),
                        " with checksum:", duplicateLetter.getChecksum());
                throw new DataIntegrityViolationException(duplicateMessage);
            }
        }
    }

    static ZonedDateTime toDateTime(LocalDateTime dateTime) {
        if (null == dateTime) {
            return null;
        }

        return dateTime.atZone(ZoneId.of("UTC"));
    }
}
