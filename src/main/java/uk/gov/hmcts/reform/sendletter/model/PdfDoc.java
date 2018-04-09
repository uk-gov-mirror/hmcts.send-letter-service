package uk.gov.hmcts.reform.sendletter.model;

public class PdfDoc {

    public final String filename;
    public final byte[] content;

    public PdfDoc(String filename, byte[] content) {
        this.filename = filename;
        this.content = content;
    }
}
