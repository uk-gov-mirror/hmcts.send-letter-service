package uk.gov.hmcts.reform.sendletter.services.util;

import uk.gov.hmcts.reform.sendletter.entity.Letter;

import java.time.format.DateTimeFormatter;

import static java.time.format.DateTimeFormatter.ofPattern;

// TODO: merge with `FileNameHelper`
public final class FinalPackageFileNameHelper {

    public static final DateTimeFormatter dateTimeFormatter = ofPattern("ddMMyyyyHHmmss");

    public static String generateName(Letter letter) {
        return String.format(
            "%s_%s_%s_%s.%s",
            letter.getType(),
            letter.getService().replace("_", ""),
            letter.getCreatedAt().toLocalDateTime().format(dateTimeFormatter),
            letter.getId(),
            letter.isEncrypted() ? "pgp" : "zip"
        );
    }

    private FinalPackageFileNameHelper() {
        // utility class constructor
    }
}
