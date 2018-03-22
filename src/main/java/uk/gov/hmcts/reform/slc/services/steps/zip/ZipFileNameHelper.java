package uk.gov.hmcts.reform.slc.services.steps.zip;

import uk.gov.hmcts.reform.sendletter.entity.Letter;

import java.time.LocalDateTime;

import static java.time.format.DateTimeFormatter.ofPattern;

public final class ZipFileNameHelper {

    public static String generateName(Letter letter, LocalDateTime timestamp) {
        return String.format(
            "%s_%s_%s_%s.zip",
            letter.getType(),
            letter.getService().replace("_", ""),
            timestamp.format(ofPattern("ddMMyyyyHHmmss")),
            letter.getId()
        );
    }

    private ZipFileNameHelper() {
        // utility class constructor
    }
}
