package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.DuplicateLetter;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.exception.ServiceNotConfiguredException;
import uk.gov.hmcts.reform.sendletter.exception.UnsupportedLetterRequestTypeException;
import uk.gov.hmcts.reform.sendletter.model.PdfDoc;
import uk.gov.hmcts.reform.sendletter.model.in.ILetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsAndNumberOfCopiesRequest;
import uk.gov.hmcts.reform.sendletter.model.in.LetterWithPdfsRequest;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;
import uk.gov.hmcts.reform.sendletter.services.encryption.UnableToLoadPgpPublicKeyException;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;

import java.io.IOException;
import java.io.Serializable;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.google.common.io.Resources.getResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LetterServiceTest {

    @Mock PdfCreator pdfCreator;
    @Mock LetterRepository letterRepository;
    @Mock Zipper zipper;
    @Mock ObjectMapper objectMapper;
    @Mock ServiceFolderMapping serviceFolderMapping;
    @Spy
    ExecusionService execusionService;
    @Mock DuplicateLetterService duplicateLetterService;

    private LetterService service;

    @ParameterizedTest
    @ValueSource(strings = {"false", "true"})
    void should_generate_final_pdf_from_template_when_old_model_is_passed(String async) {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(false, null);

        LetterRequest letter = SampleData.letterRequest();

        // when
        service.save(letter, "some_service", async);

        // then
        verify(pdfCreator).createFromTemplates(letter.documents);
        if (Boolean.parseBoolean(async)) {
            verify(execusionService).run(any(), any(), any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "true"})
    void should_handle_DataIntegrityViolationException(String async) {
        // given
        thereAreNoDuplicates();

        given(letterRepository.save(any())).willThrow(new DataIntegrityViolationException("Duplicate records"));

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(false, null);

        LetterRequest letter = SampleData.letterRequest();

        // when
        if (Boolean.parseBoolean(async)) {
            service.save(letter, "some_service", async);
        } else {
            assertThrows(DataIntegrityViolationException.class, () -> service.save(letter, "some_service", async));
        }

        // then
        verify(pdfCreator).createFromTemplates(letter.documents);
        if (Boolean.parseBoolean(async)) {
            verify(execusionService).run(any(), any(), any());
        }
        verify(duplicateLetterService).save(isA(DuplicateLetter.class));
    }


    @ParameterizedTest
    @ValueSource(strings = {"false", "true"})
    void should_generate_final_pdf_from_embedded_pdfs_when_new_model_is_passed(String async)  {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(false, null);

        LetterWithPdfsRequest letter = SampleData.letterWithPdfsRequest();

        // when
        service.save(letter, "some_service", async);

        // then
        verify(pdfCreator).createFromBase64Pdfs(letter.documents);

        if (Boolean.parseBoolean(async)) {
            verify(execusionService).run(any(), any(), any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "true"})
    void should_generate_final_pdf_with_requested_no_of_copies_when_encryption_enabled(String async) throws Exception {
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
        service.save(letterWithPdfsAndNumberOfCopiesRequest, "some_service", async);

        // then
        verify(pdfCreator).createFromBase64PdfWithCopies(letterWithPdfsAndNumberOfCopiesRequest.documents);
        verify(zipper).zip(any(PdfDoc.class));

        ArgumentCaptor<Letter> letterArgumentCaptor = ArgumentCaptor.forClass(Letter.class);
        verify(letterRepository).save(letterArgumentCaptor.capture());

        assertThat(letterArgumentCaptor.getValue().getCopies()).isEqualTo(15);

        if (Boolean.parseBoolean(async)) {
            verify(execusionService).run(any(), any(), any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "true"})
    void should_generate_final_pdf_from_template_when_old_model_is_passed_and_encryption_enabled(String async)
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
        service.save(letter, "some_service", async);

        // then
        verify(pdfCreator).createFromTemplates(letter.documents);
        verify(zipper).zip(any(PdfDoc.class));

        if (Boolean.parseBoolean(async)) {
            verify(execusionService).run(any(), any(), any());
        }

        ArgumentCaptor<Letter> letterArgumentCaptor = ArgumentCaptor.forClass(Letter.class);
        verify(letterRepository).save(letterArgumentCaptor.capture());

        assertThat(letterArgumentCaptor.getValue().getCopies()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "true"})
    void should_generate_final_pdf_from_embedded_pdfs_when_new_model_is_passed_and_encryption_enabled(String async)
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
        service.save(letter, "some_service", async);

        // then
        verify(pdfCreator).createFromBase64Pdfs(letter.documents);
        verify(zipper).zip(any(PdfDoc.class));

        if (Boolean.parseBoolean(async)) {
            verify(execusionService).run(any(), any(), any());
        }

        ArgumentCaptor<Letter> letterArgumentCaptor = ArgumentCaptor.forClass(Letter.class);
        verify(letterRepository).save(letterArgumentCaptor.capture());

        assertThat(letterArgumentCaptor.getValue().getCopies()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "true"})
    void should_generate_final_pdf_from_when_model_with_number_of_copies_is_passed(String async) throws Exception {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(true, new String(loadPublicKey()));

        LetterWithPdfsAndNumberOfCopiesRequest letter = SampleData.letterWithPdfAndCopiesRequest(3, 8);

        when(zipper.zip(any(PdfDoc.class))).thenReturn(Resources.toByteArray(getResource("unencrypted.zip")));

        // when
        service.save(letter, "some_service", async);

        // then
        verify(pdfCreator).createFromBase64PdfWithCopies(letter.documents);
        verify(zipper).zip(any(PdfDoc.class));

        if (Boolean.parseBoolean(async)) {
            verify(execusionService).run(any(), any(), any());
        }

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
    void should_throw_assertion_error_on_service_init_when_encryption_enabled_and_public_key_is_null() {
        assertThatThrownBy(() -> createLetterService(true, null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("encryptionPublicKey is null");
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "true"})
    void should_throw_an_exception_when_folder_for_given_service_is_not_configured(String async) {
        // given
        final String serviceWithoutFolderConfigured = "some_invalid_service";
        given(serviceFolderMapping.getFolderFor(serviceWithoutFolderConfigured)).willReturn(Optional.empty());
        createLetterService(false, null);

        // when
        Throwable err =
            catchThrowable(() -> service.save(SampleData.letterWithPdfsRequest(),
                    serviceWithoutFolderConfigured, async));

        // then
        assertThat(err)
            .isInstanceOf(ServiceNotConfiguredException.class)
            .hasMessageContaining(serviceWithoutFolderConfigured);

        verify(execusionService, never()).run(any(), any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "true"})
    void should_throw_an_exception_when_unsupported_letter_request_is_received(String async) {
        // given
        thereAreNoDuplicates();

        // and
        given(serviceFolderMapping.getFolderFor(any())).willReturn(Optional.of("some_folder"));
        createLetterService(false, null);

        // when
        Throwable throwable = catchThrowable(() -> service.save(new DummyLetterRequest(), "some_service", async));

        // then
        assertThat(throwable)
            .isInstanceOf(UnsupportedLetterRequestTypeException.class)
            .hasMessage("Unsupported letter request type");

        verify(execusionService, never()).run(any(), any(), any());
    }

    @Test
    void should_throw_dataIntegrityViolationException() {
        DuplicateLetter duplicateLetter = mock(DuplicateLetter.class);
        given(duplicateLetterService.isPresent(isA(UUID.class))).willReturn(Optional.of(duplicateLetter));
        createLetterService(false, null);
        UUID uuid = UUID.randomUUID();
        assertThrows(DataIntegrityViolationException.class, () -> {
            service.getStatus(uuid, "false",
                    "true");
        });
        verify(duplicateLetterService).isPresent(isA(UUID.class));
    }

    @Test
    void should_throw_LetterNotFoundException() {
        createLetterService(false, null);
        UUID id = UUID.randomUUID();
        assertThrows(LetterNotFoundException.class, () -> {
            service.getStatus(id, "false", "false");
        });
        verify(duplicateLetterService, never()).isPresent(isA(UUID.class));
    }

    @Test
    void should_return_letter() {
        createLetterService(false, null);
        ZonedDateTime now = ZonedDateTime.of(2000, 2, 12, 1, 2, 3, 123_000_000, ZoneId.systemDefault());
        Optional<Letter> letter = Optional.of(createLetter());
        given(letterRepository.findById(isA(UUID.class))).willReturn(letter);
        LetterStatus status = service.getStatus(UUID.randomUUID(), "false", "true");
        assertNotNull(status);
        verify(letterRepository).findById(isA(UUID.class));
        verify(duplicateLetterService).isPresent(isA(UUID.class));
    }


    private Letter createLetter() {
        Letter result = mock(Letter.class);
        when(result.getStatus()).thenReturn(uk.gov.hmcts.reform.sendletter.entity.LetterStatus.Created);
        return result;
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
            serviceFolderMapping,
                execusionService,
            duplicateLetterService
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
