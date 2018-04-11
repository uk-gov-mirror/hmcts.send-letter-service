package uk.gov.hmcts.reform.sendletter.services.zip;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.exception.DocumentZipException;
import uk.gov.hmcts.reform.sendletter.model.PdfDoc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class Zipper {

    private byte[] zipBytes(String filename, byte[] input) throws IOException {

        ZipEntry entry = new ZipEntry(filename);
        entry.setSize(input.length);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            zos.putNextEntry(entry);
            zos.write(input);
            zos.closeEntry();
        }

        return baos.toByteArray();
    }

    public byte[] zip(PdfDoc pdfDoc) {
        try {
            return zipBytes(pdfDoc.filename, pdfDoc.content);
        } catch (IOException exception) {
            throw new DocumentZipException(exception);
        }
    }
}
