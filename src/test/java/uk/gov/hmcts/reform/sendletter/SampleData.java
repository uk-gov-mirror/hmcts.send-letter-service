package uk.gov.hmcts.reform.sendletter;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;

import java.io.IOException;

import static com.google.common.io.Resources.getResource;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singletonList;

public final class SampleData {

    public static Letter letter() throws IOException {
        return new Letter(
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

    private SampleData() {
    }
}
