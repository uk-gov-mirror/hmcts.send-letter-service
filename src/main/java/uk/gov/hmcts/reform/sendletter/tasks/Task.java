package uk.gov.hmcts.reform.sendletter.tasks;

public enum Task {
    UploadLetters,
    MarkLettersPosted;

    int getLockId() {
        return ordinal();
    }
}
