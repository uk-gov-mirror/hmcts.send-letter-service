package uk.gov.hmcts.reform.sendletter.services;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.pdf.generator.HTMLToPDFConverter;
import uk.gov.hmcts.reform.sendletter.exception.InvalidPdfException;
import uk.gov.hmcts.reform.sendletter.services.pdf.DuplexPreparator;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;

import java.util.Base64;
import java.util.List;

import static com.google.common.io.Resources.getResource;
import static com.google.common.io.Resources.toByteArray;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class PdfCreatorTest {

    private PdfCreator pdfCreator;

    @Before
    public void setUp() {
        this.pdfCreator = new PdfCreator(new DuplexPreparator(), new HTMLToPDFConverter()::convert);
    }

    @Test
    public void should_handle_base64_encoded_pdfs() throws Exception {
        // given

        List<String> pdfs = asList(
            base64encode(toByteArray(getResource("pdfs/test1.pdf"))),
            base64encode(toByteArray(getResource("pdfs/test2.pdf")))
        );

        // when
        byte[] pdfContent = pdfCreator.createFromBase64Pdfs(pdfs);

        // then
        assertThat(pdfContent).isNotNull();
        // and no exception is thrown
    }

    @Test
    public void should_throw_an_exception_if_pdf_is_not_a_base64_string() throws Exception {
        // given
        List<String> pdfs = asList(
            "clearly not a base 64 string",
            "hello world"
        );

        // when
        Throwable exc = catchThrowable(() -> pdfCreator.createFromBase64Pdfs(pdfs));

        // then
        assertThat(exc)
            .isNotNull()
            .isInstanceOf(InvalidPdfException.class);
    }

    @Test
    public void should_throw_an_exception_if_base64_string_is_not_pdf_content() throws Exception {
        // given
        List<String> pdfs = asList(
            base64encode("i'm not a pdf".getBytes()),
            base64encode("me neither".getBytes())
        );

        // when
        Throwable exc = catchThrowable(() -> pdfCreator.createFromBase64Pdfs(pdfs));

        // then
        assertThat(exc)
            .isNotNull();
    }

    private String base64encode(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
