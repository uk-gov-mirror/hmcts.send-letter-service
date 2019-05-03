package uk.gov.hmcts.reform.sendletter.tasks.reports;

import java.io.File;

class Attachment {

    final String filename;
    final File file;

    Attachment(String filename, File file) {
        this.filename = filename;
        this.file = file;
    }
}
