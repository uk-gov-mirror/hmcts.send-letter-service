package uk.gov.hmcts.reform.sendletter.model.out;

import java.util.List;

public class LettersInfoResponse {

    public final int count;
    public final List<LetterInfo> letters;

    public LettersInfoResponse(List<LetterInfo> letters) {
        this.letters = letters;
        this.count = letters.size();
    }
}
