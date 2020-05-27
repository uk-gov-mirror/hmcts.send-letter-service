package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.pdf.generator.HTMLToPDFConverter;
import uk.gov.hmcts.reform.sendletter.PdfHelper;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;
import uk.gov.hmcts.reform.sendletter.services.pdf.DuplexPreparator;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created;
import static uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Uploaded;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class LetterServiceTest {

    private static final String SERVICE_NAME = "bulkprint";

    private LetterService service;

    @Autowired
    private LetterRepository letterRepository;

    @BeforeEach
    void setUp() {
        ServiceFolderMapping serviceFolderMapping = mock(ServiceFolderMapping.class);
        BDDMockito.given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder_name"));

        service = new LetterService(
            new PdfCreator(new DuplexPreparator(), new HTMLToPDFConverter()::convert),
            letterRepository,
            new Zipper(),
            new ObjectMapper(),
            false,
            null,
            serviceFolderMapping
        );
    }

    @AfterEach
    void tearDown() {
        letterRepository.deleteAll();
    }

    @Test
    void generates_and_saves_zipped_pdf() throws IOException {
        UUID id = service.save(SampleData.letterRequest(), SERVICE_NAME);

        Letter result = letterRepository.findById(id).get();

        assertThat(result.isEncrypted()).isFalse();
        assertThat(result.getEncryptionKeyFingerprint()).isNull();
        PdfHelper.validateZippedPdf(result.getFileContent());
    }

    @Test
    void returns_same_id_on_resubmit() {
        // given
        LetterRequest sampleRequest = SampleData.letterRequest();
        UUID id1 = service.save(sampleRequest, SERVICE_NAME);
        Letter letter = letterRepository.findById(id1).get();

        // and
        assertThat(letter.getStatus()).isEqualByComparingTo(Created);

        // when
        UUID id2 = service.save(sampleRequest, SERVICE_NAME);

        // then
        assertThat(id1).isEqualByComparingTo(id2);
    }

    @Test
    void saves_an_new_letter_if_previous_one_has_been_sent_to_print() {
        // given
        LetterRequest sampleRequest = SampleData.letterRequest();
        UUID id1 = service.save(sampleRequest, SERVICE_NAME);
        Letter letter = letterRepository.findById(id1).get();

        // and
        assertThat(letter.getStatus()).isEqualByComparingTo(Created);

        // when
        letter.setStatus(Uploaded);
        letterRepository.saveAndFlush(letter);
        UUID id2 = service.save(sampleRequest, SERVICE_NAME);

        // then
        assertThat(id1).isNotEqualByComparingTo(id2);
    }

    @Test
    void should_not_allow_null_service_name() {
        assertThatThrownBy(() -> service.save(SampleData.letterRequest(), null))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void should_not_allow_empty_service_name() {
        assertThatThrownBy(() -> service.save(SampleData.letterRequest(), ""))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void handles_null_timestamps() {
        assertThat(LetterService.toDateTime(null)).isNull();
    }
}
