package uk.gov.hmcts.reform.slc.services.steps.getpdf;

import org.apache.http.util.Asserts;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pdf.generator.HTMLToPDFConverter;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.duplex.DuplexPreparator;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class PdfCreator {

    private final DuplexPreparator duplexPreparator;
    private static HTMLToPDFConverter converter = new HTMLToPDFConverter();

    public PdfCreator(DuplexPreparator duplexPreparator) {
        this.duplexPreparator = duplexPreparator;
    }

    public byte[] create(LetterRequest letter) {
        Asserts.notNull(letter, "letter");

        List<byte[]> docs =
            letter.documents
                .stream()
                .map(this::generatePdf)
                .map(duplexPreparator::prepare)
                .collect(toList());

        return PdfMerger.mergeDocuments(docs);
    }

    private byte[] generatePdf(Document document) {
        synchronized (PdfCreator.class) {
            return converter.convert(document.template.getBytes(), document.values);
        }
    }
}
