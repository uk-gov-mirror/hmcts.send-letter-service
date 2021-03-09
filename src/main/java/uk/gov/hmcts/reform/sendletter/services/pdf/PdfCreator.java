package uk.gov.hmcts.reform.sendletter.services.pdf;

import org.apache.http.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.model.in.Doc;
import uk.gov.hmcts.reform.sendletter.model.in.Document;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class PdfCreator {
    private static final Logger logger = LoggerFactory.getLogger(PdfCreator.class);
    private final DuplexPreparator duplexPreparator;
    private final IHtmlToPdfConverter converter;

    public PdfCreator(DuplexPreparator duplexPreparator, IHtmlToPdfConverter converter) {
        this.duplexPreparator = duplexPreparator;
        this.converter = converter;
    }

    public byte[] createFromTemplates(List<Document> documents) {
        Asserts.notNull(documents, "documents");

        List<byte[]> docs =
            documents
                .stream()
                .map(this::generatePdf)
                .map(duplexPreparator::prepare)
                .collect(toList());

        return PdfMerger.mergeDocuments(docs);
    }

    public byte[] createFromBase64Pdfs(List<byte[]> base64decodedDocs) {
        Asserts.notNull(base64decodedDocs, "base64decodedDocs");

        List<byte[]> docs = base64decodedDocs
            .stream()
            .map(duplexPreparator::prepare)
            .collect(toList());

        return PdfMerger.mergeDocuments(docs);
    }

    public byte[] createFromBase64PdfWithCopies(List<Doc> docs) {
        Asserts.notNull(docs, "base64decodedDocs");

        List<byte[]> pdfs = docs
            .stream()
            .map(doc -> new Doc(duplexPreparator.prepare(doc.content), doc.copies))
            .map(d -> Collections.nCopies(d.copies, d.content))
            .flatMap(Collection::stream)
            .peek(data -> logger.info("Final file size {} KB", data.length / 1024))
            .collect(toList());

        return PdfMerger.mergeDocuments(pdfs);
    }

    private byte[] generatePdf(Document document) {
        synchronized (PdfCreator.class) {
            return converter.apply(document.template.getBytes(), document.values);
        }
    }
}
