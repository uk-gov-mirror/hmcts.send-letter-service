package uk.gov.hmcts.reform.sendletter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.slc.model.LetterPrintStatus;
import uk.gov.hmcts.reform.slc.services.steps.sftpupload.ParsedReport;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public final class SampleData {

    public static LetterRequest letterRequest() throws IOException {
        return new LetterRequest(
            singletonList(
                new Document(
                    Resources.toString(getResource("template.html"), UTF_8),
                    ImmutableMap.of(
                        "name", "John",
                        "reference", UUID.randomUUID()
                    )
                )
            ),
            "someType",
            Maps.newHashMap()
        );
    }

    public static LetterWithPdfsRequest letterWithPdfsRequest() throws IOException {
        return new LetterWithPdfsRequest(
            singletonList(
                Base64.getEncoder().encodeToString("hello world".getBytes())
            ),
            "someType",
            Maps.newHashMap()
        );
    }

    public static uk.gov.hmcts.reform.sendletter.entity.Letter letterEntity(String service) {
        try {
            return new uk.gov.hmcts.reform.sendletter.entity.Letter(
                UUID.randomUUID(),
                "messageId",
                service,
                new ObjectMapper().readTree("{}"),
                "a type",
                new byte[1],
                false
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ParsedReport parsedReport(String filename, List<UUID> letterIds, boolean allParsed) {
        return new ParsedReport(
            filename,
            letterIds
                .stream()
                .map(id -> new LetterPrintStatus(id, ZonedDateTime.now()))
                .collect(toList()),
            allParsed
        );
    }

    public static ParsedReport parsedReport(String filename, boolean allParsed) {
        return parsedReport(filename, Arrays.asList(UUID.randomUUID(), UUID.randomUUID()), allParsed);
    }

    private SampleData() {
    }
}
