package uk.gov.hmcts.reform.slc.services.steps.getpdf;

import com.google.common.io.ByteStreams;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sendletter.model.in.Document;
import uk.gov.hmcts.reform.slc.services.steps.getpdf.duplex.DuplexPreparator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class PdfCreatorTest {

    @Mock private DuplexPreparator duplexPreparator;
    @Mock private IHtmlToPdfConverter converter;

    private PdfCreator pdfCreator;

    @Before
    public void setUp() {
        pdfCreator = new PdfCreator(this.duplexPreparator, this.converter);
    }

    @Test
    public void should_require_documents_to_not_be_null() {
        assertThatThrownBy(() -> pdfCreator.createFromTemplates(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("documents");
    }

    @Test
    public void should_return_a_merged_pdf_when_multiple_documents_are_passed() throws Exception {
        byte[] test1Pdf = toByteArray(getResource("test1.pdf"));
        byte[] test2Pdf = toByteArray(getResource("test2.pdf"));
        byte[] expectedMergedPdf = toByteArray(getResource("merged.pdf"));

        given(duplexPreparator.prepare(test1Pdf)).willReturn(test1Pdf);
        given(duplexPreparator.prepare(test2Pdf)).willReturn(test2Pdf);

        given(converter.apply(eq("t1".getBytes()), any())).willReturn(test1Pdf);
        given(converter.apply(eq("t2".getBytes()), any())).willReturn(test2Pdf);

        List<Document> docs = asList(
            new Document("t1", emptyMap()),
            new Document("t2", emptyMap())
        );

        // when
        byte[] pdfContent = pdfCreator.createFromTemplates(docs);

        // then
        InputStream actualPdfPage1 = getPdfPageContents(pdfContent, 0);
        InputStream actualPdfPage2 = getPdfPageContents(pdfContent, 1);

        InputStream expectedPdfPage1 = getPdfPageContents(expectedMergedPdf, 0);
        InputStream expectedPdfPage2 = getPdfPageContents(expectedMergedPdf, 1);

        assertThat(actualPdfPage1).hasSameContentAs(expectedPdfPage1);
        assertThat(actualPdfPage2).hasSameContentAs(expectedPdfPage2);

        verify(duplexPreparator, times(2)).prepare(any(byte[].class));
    }

    private InputStream getPdfPageContents(byte[] pdf, int pageNumber) throws Exception {
        try (PDDocument doc = PDDocument.load(pdf)) {
            byte[] data = ByteStreams.toByteArray(doc.getPage(pageNumber).getContents());
            return new ByteArrayInputStream(data);
        }
    }

}
