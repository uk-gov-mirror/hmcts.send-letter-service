package uk.gov.hmcts.reform.sendletter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import uk.gov.hmcts.reform.pdf.generator.HTMLToPDFConverter;
import uk.gov.hmcts.reform.sendletter.SampleData;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.model.in.LetterRequest;
import uk.gov.hmcts.reform.sendletter.services.encryption.PgpDecryptionHelper;
import uk.gov.hmcts.reform.sendletter.services.ftp.ServiceFolderMapping;
import uk.gov.hmcts.reform.sendletter.services.pdf.DuplexPreparator;
import uk.gov.hmcts.reform.sendletter.services.pdf.PdfCreator;
import uk.gov.hmcts.reform.sendletter.services.zip.Zipper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class LetterServiceWithEncryptionEnabledTest {

    private static final String SERVICE_NAME = "a_service";
    private static final String KEY_FINGERPRINT = "9c61b7da4e6c94416be51136122ed01acea9884f";

    @Value("${encryption.publicKey}")
    private String encryptionPublicKey;

    @Autowired
    private LetterRepository letterRepository;

    @Mock
    private ServiceFolderMapping serviceFolderMapping;

    @AfterEach
    void tearDown() {
        letterRepository.deleteAll();
    }

    @Test
    void generates_and_saves_encrypted_zip_when_encryption_is_enabled() throws Exception {
        when(serviceFolderMapping.getFolderFor(any())).thenReturn(Optional.of("some_folder"));

        LetterRequest letterRequest = SampleData.letterRequest();

        PdfCreator pdfCreator = new PdfCreator(new DuplexPreparator(), new HTMLToPDFConverter()::convert);

        LetterService service = new LetterService(
            pdfCreator,
            letterRepository,
            new Zipper(),
            new ObjectMapper(),
            true,
            encryptionPublicKey,
            serviceFolderMapping
        );

        UUID id = service.save(letterRequest, SERVICE_NAME);

        Letter letterInDb = letterRepository.findById(id).get();

        assertThat(letterInDb.isEncrypted()).isTrue();
        assertThat(letterInDb.getEncryptionKeyFingerprint()).isEqualTo(KEY_FINGERPRINT);

        byte[] encryptedZip = letterInDb.getFileContent();

        PgpDecryptionHelper.DecryptedFile decryptedZip = PgpDecryptionHelper.decryptFile(
            encryptedZip,
            getClass().getResourceAsStream("/encryption/privatekey.asc"),
            "Password1".toCharArray()
        );
        //then
        //We don't have the original zip so just verifying if we could decrypt the encrypted file.
        assertThat(decryptedZip.content).isNotEmpty();
    }
}
