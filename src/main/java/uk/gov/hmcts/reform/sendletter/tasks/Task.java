package uk.gov.hmcts.reform.sendletter.tasks;

public enum Task {
    UploadLetters,
    MarkLettersPosted;

    long getLockId() {
        return ordinal();
    }
}
