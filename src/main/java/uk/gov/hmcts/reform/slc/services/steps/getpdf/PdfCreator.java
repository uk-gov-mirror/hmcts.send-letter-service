package uk.gov.hmcts.reform.slc.services.steps.getpdf;

import org.apache.http.util.Asserts;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pdf.generator.HTMLToPDFConverter;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.sendletter.model.in.Letter;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.duplex.DuplexPreparator;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Service
public class PdfCreator {

    private final DuplexPreparator duplexPreparator;

    public PdfCreator(DuplexPreparator duplexPreparator) {
        this.duplexPreparator = duplexPreparator;
    }

    private static HTMLToPDFConverter converter = new HTMLToPDFConverter();

    public static synchronized byte[] generatePdf(byte[] template, Map<String, Object> content) {
        return converter.convert(template, content);
    }

    private byte[] generatePdf(Document document) {
        Instant start = Instant.now();
        byte[] pdf = generatePdf(document.template.getBytes(), document.values);
        return pdf;
    }

    public byte[] create(Letter letter) {
        Asserts.notNull(letter, "letter");

        List<byte[]> docs =
            letter.documents
                .stream()
                .map(this::generatePdf)
                .map(duplexPreparator::prepare)
                .collect(toList());

        return PdfMerger.mergeDocuments(docs);
    }

    public PdfDoc create(Letter letter, String service, String id) {
        Asserts.notNull(letter, "letter");

        byte[] finalContent = create(letter);
        return new PdfDoc(
            FileNameHelper.generateName(letter, "pdf", service, id),
            finalContent
        );
    }
}
