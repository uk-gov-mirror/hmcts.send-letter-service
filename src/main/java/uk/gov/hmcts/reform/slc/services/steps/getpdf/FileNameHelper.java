package uk.gov.hmcts.reform.slc.services.steps.getpdf;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.io.FilenameUtils;
import uk.gov.hmcts.reform.sendletter.entity.Letter;

import java.util.UUID;

public final class FileNameHelper {

    private static final String SEPARATOR = "_";

    public static String generateName(Letter letter, String extension) {
        String serviceName = letter.getService().replace(SEPARATOR, "");
        return letter.getType() + SEPARATOR + serviceName + SEPARATOR + letter.getId() + "." + extension;
    }

    public static UUID extractId(String fileName) {
        String[] parts = FilenameUtils.removeExtension(fileName).split(SEPARATOR);
        if (parts.length < 3) {
            throw new UnableToExtractIdFromFileNameException("Invalid filename " + fileName);
        } else {
            try {
                return UUID.fromString(parts[parts.length - 1]);
            } catch (IllegalArgumentException e) {
                throw new UnableToExtractIdFromFileNameException(e);
            }
        }
    }

    private FileNameHelper() {
    }

}
