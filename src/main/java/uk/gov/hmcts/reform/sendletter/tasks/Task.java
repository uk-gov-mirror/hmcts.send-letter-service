package uk.gov.hmcts.reform.sendletter.tasks;

public enum Task {
    UploadLetters,
    MarkLettersPosted,
    StaleLetters;

    int getLockId() {
        return ordinal();
    }
}
