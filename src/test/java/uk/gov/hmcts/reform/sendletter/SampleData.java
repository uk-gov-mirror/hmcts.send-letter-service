package uk.gov.hmcts.reform.sendletter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SampleData {

    public static Letter letter() {
        Map<String, Object> content = ImmutableMap.of("name", "John");
        List<Document> documents = Lists.newArrayList(
            new Document(readResource("template.html"),
                ImmutableMap.of("name", "John"))
        );
        return new Letter(documents, "a type", Maps.newHashMap());
    }

    private static String readResource(String name) {
        try {
            return Resources.toString(Resources.getResource("template.html"),
                StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    private SampleData() {
    }
}
