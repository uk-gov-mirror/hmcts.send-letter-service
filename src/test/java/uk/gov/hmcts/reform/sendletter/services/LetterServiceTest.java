package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.exception.ServiceNotConfiguredException;
import uk.gov.hmcts.reform.sendletter.exception.UnsupportedLetterRequestTypeException;
import uk.gov.hmcts.reform.sendletter.model.PdfDoc;
import uk.gov.hmcts.reform.sendletter.model.in.ILetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsAndNumberOfCopiesRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.services.encryption.UnableToLoadPgpPublicKeyException;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LetterServiceTest {

    @Mock PdfCreator pdfCreator;
    @Mock LetterRepository letterRepository;
    @Mock Zipper zipper;
    @Mock ObjectMapper objectMapper;
    @Mock ServiceFolderMapping serviceFolderMapping;

    private LetterService service;

    @Test
    void should_generate_final_pdf_from_template_when_old_model_is_passed() throws Exception {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(false, null);

        LetterRequest letter = SampleData.letterRequest();

        // when
        service.save(letter, "some_service");

        // then
        verify(pdfCreator).createFromTemplates(letter.documents);
    }

    @Test
    void should_generate_final_pdf_with_requested_no_of_copies_when_encryption_enabled() throws Exception {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(false, new String(loadPublicKey()));

        final LetterWithPdfsAndNumberOfCopiesRequest letterWithPdfsAndNumberOfCopiesRequest =
                SampleData.letterWithPdfAndCopiesRequest(5, 10);

        byte[] inputZipFile = Resources.toByteArray(getResource("unencrypted.zip"));

        when(zipper.zip(any(PdfDoc.class))).thenReturn(inputZipFile);

        // when
        service.save(letterWithPdfsAndNumberOfCopiesRequest, "some_service");

        // then
        verify(pdfCreator).createFromBase64PdfWithCopies(letterWithPdfsAndNumberOfCopiesRequest.documents);
        verify(zipper).zip(any(PdfDoc.class));

        ArgumentCaptor<Letter> letterArgumentCaptor = ArgumentCaptor.forClass(Letter.class);
        verify(letterRepository).save(letterArgumentCaptor.capture());

        assertThat(letterArgumentCaptor.getValue().getCopies()).isEqualTo(15);
    }

    @Test
    void should_generate_final_pdf_from_embedded_pdfs_when_new_model_is_passed() throws Exception {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(false, null);

        LetterWithPdfsRequest letter = SampleData.letterWithPdfsRequest();

        // when
        service.save(letter, "some_service");

        // then
        verify(pdfCreator).createFromBase64Pdfs(letter.documents);

        ArgumentCaptor<Letter> letterArgumentCaptor = ArgumentCaptor.forClass(Letter.class);
        verify(letterRepository).save(letterArgumentCaptor.capture());

        assertThat(letterArgumentCaptor.getValue().getCopies()).isEqualTo(1);
    }

    @Test
    void should_generate_final_pdf_from_template_when_old_model_is_passed_and_encryption_enabled()
        throws Exception {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(true, new String(loadPublicKey()));

        LetterRequest letter = SampleData.letterRequest();

        byte[] inputZipFile = Resources.toByteArray(getResource("unencrypted.zip"));

        when(zipper.zip(any(PdfDoc.class))).thenReturn(inputZipFile);

        // when
        service.save(letter, "some_service");

        // then
        verify(pdfCreator).createFromTemplates(letter.documents);
        verify(zipper).zip(any(PdfDoc.class));

        ArgumentCaptor<Letter> letterArgumentCaptor = ArgumentCaptor.forClass(Letter.class);
        verify(letterRepository).save(letterArgumentCaptor.capture());

        assertThat(letterArgumentCaptor.getValue().getCopies()).isEqualTo(1);
    }

    @Test
    void should_generate_final_pdf_from_embedded_pdfs_when_new_model_is_passed_and_encryption_enabled()
        throws Exception {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(true, new String(loadPublicKey()));

        LetterWithPdfsRequest letter = SampleData.letterWithPdfsRequest();

        byte[] inputZipFile = Resources.toByteArray(getResource("unencrypted.zip"));

        when(zipper.zip(any(PdfDoc.class))).thenReturn(inputZipFile);

        // when
        service.save(letter, "some_service");

        // then
        verify(pdfCreator).createFromBase64Pdfs(letter.documents);
        verify(zipper).zip(any(PdfDoc.class));

        ArgumentCaptor<Letter> letterArgumentCaptor = ArgumentCaptor.forClass(Letter.class);
        verify(letterRepository).save(letterArgumentCaptor.capture());

        assertThat(letterArgumentCaptor.getValue().getCopies()).isEqualTo(1);
    }

    @Test
    void should_generate_final_pdf_from_when_model_with_number_of_copies_is_passed() throws Exception {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(true, new String(loadPublicKey()));

        LetterWithPdfsAndNumberOfCopiesRequest letter = SampleData.letterWithPdfAndCopiesRequest(3, 8);

        when(zipper.zip(any(PdfDoc.class))).thenReturn(Resources.toByteArray(getResource("unencrypted.zip")));

        // when
        service.save(letter, "some_service");

        // then
        verify(pdfCreator).createFromBase64PdfWithCopies(letter.documents);
        verify(zipper).zip(any(PdfDoc.class));

        ArgumentCaptor<Letter> letterArgumentCaptor = ArgumentCaptor.forClass(Letter.class);
        verify(letterRepository).save(letterArgumentCaptor.capture());

        assertThat(letterArgumentCaptor.getValue().getCopies()).isEqualTo(11);
    }

    @Test
    void should_throw_unable_to_load_pgp_pub_key_exc_on_init_when_enc_enabled_and_invalid_pub_key_is_passed() {
        assertThatThrownBy(() -> createLetterService(true, "This is not a proper pgp public key"))
            .isInstanceOf(UnableToLoadPgpPublicKeyException.class);
    }

    @Test
    public void should_throw_assertion_error_on_service_init_when_encryption_enabled_and_public_key_is_null() {
        assertThatThrownBy(() -> createLetterService(true, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("encryptionPublicKey is null");
    }

    @Test
    public void should_throw_an_exception_when_folder_for_given_service_is_not_configured() {
        // given
        final String serviceWithoutFolderConfigured = "some_invalid_service";
        given(serviceFolderMapping.getFolderFor(serviceWithoutFolderConfigured)).willReturn(Optional.empty());
        createLetterService(false, null);

        // when
        Throwable err =
            catchThrowable(() -> service.save(SampleData.letterWithPdfsRequest(), serviceWithoutFolderConfigured));

        // then
        assertThat(err)
            .isInstanceOf(ServiceNotConfiguredException.class)
            .hasMessageContaining(serviceWithoutFolderConfigured);
    }

    @Test
    void should_throw_an_exception_when_unsupported_letter_request_is_received() {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(false, null);

        // when
        Throwable throwable = catchThrowable(() -> service.save(new DummyLetterRequest(), "some_service"));

        // then
        assertThat(throwable)
            .isInstanceOf(UnsupportedLetterRequestTypeException.class)
            .hasMessage("Unsupported letter request type");
    }

    private void thereAreNoDuplicates() {
        given(letterRepository.findByChecksumAndStatusOrderByCreatedAtDesc(any(), any()))
            .willReturn(Optional.empty());
    }

    private void createLetterService(Boolean isEncryptionEnabled, String encryptionKey) {
        this.service = new LetterService(
            pdfCreator,
            letterRepository,
            zipper,
            objectMapper,
            isEncryptionEnabled,
            encryptionKey,
            serviceFolderMapping
        );
    }

    private byte[] loadPublicKey() throws IOException {
        return Resources.toByteArray(getResource("encryption/pubkey.asc"));
    }

    private static class DummyLetterRequest implements ILetterRequest, Serializable {
        private static final long serialVersionUID = -7737087336283080071L;

        @Override
        public String getType() {
            return "dummy";
        }

        @Override
        public Map<String, Object> getAdditionalData() {
            return Collections.emptyMap();
        }
    }
}
