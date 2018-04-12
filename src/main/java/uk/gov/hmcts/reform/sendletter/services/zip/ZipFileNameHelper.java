package uk.gov.hmcts.reform.sendletter.services.zip;

import uk.gov.hmcts.reform.sendletter.entity.Letter;

import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;

public final class ZipFileNameHelper {

    public static final DateTimeFormatter dateTimeFormatter = ofPattern("ddMMyyyyHHmmss");

    public static String generateName(Letter letter) {
        return String.format(
            "%s_%s_%s_%s.zip",
            letter.getType(),
            letter.getService().replace("_", ""),
            letter.getCreatedAt().toLocalDateTime().format(dateTimeFormatter),
            letter.getId()
        );
    }

    private ZipFileNameHelper() {
        // utility class constructor
    }
}
