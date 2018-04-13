package uk.gov.hmcts.reform.sendletter.services.util;

import uk.gov.hmcts.reform.sendletter.entity.Letter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static java.time.format.DateTimeFormatter.ofPattern;

// TODO: merge with `FileNameHelper`
public final class FinalPackageFileNameHelper {

    public static final DateTimeFormatter dateTimeFormatter = ofPattern("ddMMyyyyHHmmss");

    public static String generateName(Letter letter) {
        return generateName(
            letter.getType(),
            letter.getService(),
            letter.getCreatedAt().toLocalDateTime(),
            letter.getId(),
            letter.isEncrypted()
        );
    }

    public static String generateName(
        String type,
        String service,
        LocalDateTime createdAtDateTime,
        UUID id,
        Boolean isEncrypted
    ) {
        return String.format(
            "%s_%s_%s_%s.%s",
            type,
            service.replace("_", ""),
            createdAtDateTime.format(dateTimeFormatter),
            id,
            isEncrypted ? "pgp" : "zip"
        );
    }

    private FinalPackageFileNameHelper() {
        // utility class constructor
    }
}
