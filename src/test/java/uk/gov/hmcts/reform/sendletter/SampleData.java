package uk.gov.hmcts.reform.sendletter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;

import java.io.IOException;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;

public final class SampleData {

    public static LetterRequest letter() throws IOException {
        return new LetterRequest(
            singletonList(
                new Document(
                    Resources.toString(getResource("template.html"), UTF_8),
                    ImmutableMap.of("name", "John")
                )
            ),
            "someType",
            Maps.newHashMap()
        );
    }

    public static uk.gov.hmcts.reform.sendletter.entity.Letter letterEntity(String service) {
        try {
            return new uk.gov.hmcts.reform.sendletter.entity.Letter(
                "messageId",
                service,
                new ObjectMapper().readTree("{}"),
                "a type",
                new byte[1]
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SampleData() {
    }
}
