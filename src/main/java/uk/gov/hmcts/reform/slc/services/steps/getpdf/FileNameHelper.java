package uk.gov.hmcts.reform.slc.services.steps.getpdf;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.io.FilenameUtils;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;

public final class FileNameHelper {

    private static final String SEPARATOR = "_";

    public static String generateName(Letter letter, String extension, String service, String id) {
        return letter.type + SEPARATOR + service + SEPARATOR + id + "." + extension;
    }

    public static String extractId(String fileName) {
        String[] parts = FilenameUtils.removeExtension(fileName).split(SEPARATOR);
        if (parts.length != 3) {
            throw new UnableToExtractIdFromFileNameException();
        } else {
            return parts[2];
        }
    }

    private FileNameHelper() {
    }

    public static class UnableToExtractIdFromFileNameException extends RuntimeException {

    }
}
