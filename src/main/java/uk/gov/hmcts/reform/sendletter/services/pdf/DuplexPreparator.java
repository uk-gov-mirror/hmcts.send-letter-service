package uk.gov.hmcts.reform.sendletter.services.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sendletter.exception.DuplexException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
public class DuplexPreparator {

    /**
     * Adds an extra blank page if the total number of pages is odd.
     */
    public byte[] prepare(byte[] pdf) {
        try (PDDocument pdDoc = PDDocument.load(pdf)) {
            if (pdDoc.getNumberOfPages() % 2 == 1) {
                if(pdDoc.isEncrypted()) {
                    pdDoc.setAllSecurityToBeRemoved(true);
                }
                PDRectangle lastPageMediaBox = pdDoc.getPage(pdDoc.getNumberOfPages() - 1).getMediaBox();
                pdDoc.addPage(new PDPage(lastPageMediaBox));
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                pdDoc.save(out);
                if(pdDoc.isEncrypted()) {
                    AccessPermission ap = new AccessPermission();
                    StandardProtectionPolicy spp = new StandardProtectionPolicy("", "", ap);
                    pdDoc.protect(spp);
                }
                pdDoc.close();
                return out.toByteArray();

            } else {
                return pdf;
            }
        } catch (IOException exc) {
            throw new DuplexException(exc);
        }
    }
}
