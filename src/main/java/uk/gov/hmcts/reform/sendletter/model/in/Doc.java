package uk.gov.hmcts.reform.sendletter.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class Doc implements Serializable {

    private static final long serialVersionUID = -1718267310344700595L;

    public final byte[] content;
    public final int copies;

    public Doc(
        @JsonProperty("content") byte[] content,
        @JsonProperty("copies") int copies
    ) {
        this.content = content;
        this.copies = copies;
    }
}
