package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LetterServiceTest {

    @Mock PdfCreator pdfCreator;
    @Mock LetterRepository letterRepository;
    @Mock Zipper zipper;
    @Mock ObjectMapper objectMapper;

    private LetterService service;

    @Before
    public void setUp() throws Exception {
        this.service = new LetterService(
            pdfCreator,
            letterRepository,
            zipper,
            objectMapper,
            false
        );
    }

    @Test
    public void should_generate_final_pdf_from_template_when_old_model_is_passed() throws Exception {
        // given
        thereAreNoDuplicates();
        LetterRequest letter = SampleData.letterRequest();

        // when
        service.send(letter, "some_service");

        // then
        verify(pdfCreator).createFromTemplates(letter.documents);
    }

    @Test
    public void should_generate_final_pdf_from_embedded_pdfs_when_new_model_is_passed() throws Exception {
        // given
        thereAreNoDuplicates();
        LetterWithPdfsRequest letter = SampleData.letterWithPdfsRequest();

        // when
        service.send(letter, "some_service");

        // then
        verify(pdfCreator).createFromBase64Pdfs(letter.documents);
    }

    private void thereAreNoDuplicates() {
        given(letterRepository.findByMessageIdAndStatusOrderByCreatedAtDesc(any(), any()))
            .willReturn(Optional.empty());
    }
}
