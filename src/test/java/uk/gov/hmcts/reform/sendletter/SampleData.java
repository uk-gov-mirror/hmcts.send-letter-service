package uk.gov.hmcts.reform.sendletter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import uk.gov.hmcts.reform.sendletter.entity.Print;
import uk.gov.hmcts.reform.sendletter.model.LetterPrintStatus;
import uk.gov.hmcts.reform.sendletter.model.ParsedReport;
import uk.gov.hmcts.reform.sendletter.model.in.Doc;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsAndNumberOfCopiesRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsRequest;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static java.time.LocalDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static uk.gov.hmcts.reform.sendletter.util.ResourceLoader.loadJson;
import static uk.gov.hmcts.reform.sendletter.util.ResourceLoader.loadResource;

public final class SampleData {
    private static final String ENCODED_PDF_FILE = "encodedPdfFile.txt";
    private static final String ENCODED_PDF_FILE2 = "encodedPdfFile2.txt";
    public static final Supplier<String> checkSumSupplier = () -> UUID.randomUUID().toString();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static LetterRequest letterRequest() {
        try {
            return new LetterRequest(
                singletonList(
                    new Document(
                        loadJson("template.html"),
                        ImmutableMap.of(
                            "name", "John",
                            "reference", UUID.randomUUID()
                        )
                    )
                ),
                "someType",
                Maps.newHashMap()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static LetterWithPdfsRequest letterWithPdfsRequest() {
        return new LetterWithPdfsRequest(
            singletonList(
                Base64.getEncoder().encode("hello world".getBytes())
            ),
            "someType",
            Maps.newHashMap()
        );
    }

    public static LetterWithPdfsAndNumberOfCopiesRequest letterWithPdfAndCopiesRequest(int copies1, int copies2)
        throws Exception {
        return new LetterWithPdfsAndNumberOfCopiesRequest(
            asList(
                new Doc(Base64.getDecoder().decode(loadResource(ENCODED_PDF_FILE)), copies1),
                new Doc(Base64.getDecoder().decode(loadResource(ENCODED_PDF_FILE2)), copies2)
            ),
            "some_type",
            Map.of("caseid",1111)
        );
    }

    public static uk.gov.hmcts.reform.sendletter.entity.Letter letterEntity(String service) {
        return letterEntity(service, now());
    }

    public static uk.gov.hmcts.reform.sendletter.entity.Letter letterEntity(String service, Supplier<String> checkSum) {
        return letterEntity(service, now(), "letterType1",null, Map.of("Document_1", 1), checkSum);
    }

    public static uk.gov.hmcts.reform.sendletter.entity.Letter letterEntity(String service, LocalDateTime createdAt) {
        return letterEntity(service, createdAt, "letterType1", null, Map.of("Document_1", 1), checkSumSupplier);
    }

    public static uk.gov.hmcts.reform.sendletter.entity.Letter letterEntity(
        String service,
        LocalDateTime createdAt,
        String type
    ) {
        return letterEntity(service, createdAt, type, null, Map.of("Document_1", 1), checkSumSupplier);
    }

    public static uk.gov.hmcts.reform.sendletter.entity.Letter letterEntity(
        String service,
        LocalDateTime createdAt,
        String type,
        String fingerprint,
        Map<String, Integer> copies,
        Supplier<String> checkSum
    ) {

        try {
            return new uk.gov.hmcts.reform.sendletter.entity.Letter(
                UUID.randomUUID(),
                checkSum.get(),
                service,
                objectMapper.readTree("{}"),
                type,
                new byte[1],
                false,
                fingerprint,
                createdAt,
                objectMapper.valueToTree(copies)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static uk.gov.hmcts.reform.sendletter.entity.Letter letterEntity(
        String service,
        LocalDateTime createdAt,
        String type,
        String fingerprint,
        Map<String, Integer> copies,
        Supplier<String> checkSum,
        Map<String, Object> additionalData
    ) {

        try {
            return new uk.gov.hmcts.reform.sendletter.entity.Letter(
                UUID.randomUUID(),
                checkSum.get(),
                service,
                objectMapper.readTree(objectMapper.writeValueAsString(additionalData)),
                type,
                new byte[1],
                false,
                fingerprint,
                createdAt,
                objectMapper.valueToTree(copies)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static uk.gov.hmcts.reform.sendletter.entity.Letter letterEntityWithRecipients(
        String service, LocalDateTime createdAt, List<String> recipients) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("recipients", recipients);
        return letterEntity(service, createdAt, "letterType1",
            null, Map.of("Document_1", 1), checkSumSupplier, additionalData);
    }

    public static uk.gov.hmcts.reform.sendletter.entity.Document documentEntity(
        UUID letterId,
        String checkSum,
        String recipientsChecksum,
        LocalDateTime createdAt
    ) {
        return new uk.gov.hmcts.reform.sendletter.entity.Document(
            UUID.randomUUID(),
            letterId,
            checkSum,
            recipientsChecksum,
            createdAt
        );
    }

    public static ParsedReport parsedReport(String filename, List<UUID> letterIds, boolean allParsed) {
        return new ParsedReport(
            filename,
            letterIds
                .stream()
                .map(id -> new LetterPrintStatus(id, ZonedDateTime.now()))
                .collect(toList()),
            allParsed,
            ZonedDateTime.now().toLocalDate()
        );
    }

    public static ParsedReport parsedReport(String filename, boolean allParsed) {
        return parsedReport(filename, asList(UUID.randomUUID(), UUID.randomUUID()), allParsed);
    }

    public static LetterWithPdfsRequest letterWithPdfsRequestWithAdditionalData() throws Exception {
        return new LetterWithPdfsRequest(
                singletonList(
                        Base64.getDecoder().decode(loadResource(ENCODED_PDF_FILE))), "someType",
                Map.of("reference", "ABD-123-WAZ",
                        "count", 10,
                        "additionInfo", "present")
        );
    }

    public static LetterWithPdfsRequest letterWithPdfsRequestWithAdditionalDataIncludingRecipients() throws Exception {
        return new LetterWithPdfsRequest(
            singletonList(
                Base64.getDecoder().decode(loadResource(ENCODED_PDF_FILE))), "someType",
            Map.of("reference", "ABD-123-WAZ",
                "count", 10,
                "recipients", Arrays.asList("one", "two"))
        );
    }

    public static LetterWithPdfsRequest letterWithPdfsRequestWithNoAdditionalData() throws Exception {
        return new LetterWithPdfsRequest(
                singletonList(
                        Base64.getDecoder().decode(loadResource(ENCODED_PDF_FILE))
                ),
                "someType",
                null
        );
    }

    private SampleData() {
    }

    public static Print printEntity(
        UUID id,
        String service,
        LocalDateTime createdAt,
        String type,
        String idempotencyKey,
        JsonNode documents,
        String caseId,
        String caseRef,
        String letterType
    ) {
        return new Print(
            id,
            service,
            createdAt,
            type,
            idempotencyKey,
            documents,
            caseId,
            caseRef,
            letterType
        );
    }
}
