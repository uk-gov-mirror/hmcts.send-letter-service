package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.exception.UnableToExtractIdFromFileNameException;
import uk.gov.hmcts.reform.sendletter.services.util.FileNameHelper;

import java.util.UUID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileNameHelperTest {

    @Test
    void should_generate_file_name_in_expected_format() {
        // given
        UUID letterId = UUID.randomUUID();
        Letter letter = createLetter(letterId, "typeA", "cmc");

        // when
        String result = FileNameHelper.generatePdfName(letter);

        // then
        assertThat(result).isEqualTo("typeA_cmc_" + letterId + ".pdf");
    }

    @Test
    void should_always_generate_the_same_name_for_same_letter() {
        // given
        UUID letterId = UUID.randomUUID();
        Letter letter1 = createLetter(letterId, "A", "B");
        Letter letter2 = createLetter(letterId, "A", "B");

        String result1 = FileNameHelper.generatePdfName(letter1);
        String result2 = FileNameHelper.generatePdfName(letter2);

        // then
        assertThat(result1).isEqualTo(result2);
    }

    @Test
    void should_generate_different_names_for_different_letters() {
        // given
        Letter letter1 = createLetter(UUID.randomUUID(), "A", "B");
        Letter letter2 = createLetter(UUID.randomUUID(), "C", "D");

        String result1 = FileNameHelper.generatePdfName(letter1);
        String result2 = FileNameHelper.generatePdfName(letter2);

        // then
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void should_generate_different_names_for_same_letters_with_different_id() {
        // given
        Letter letter1 = createLetter(UUID.randomUUID(), "A", "B");
        Letter letter2 = createLetter(UUID.randomUUID(), "A", "B");

        String result1 = FileNameHelper.generatePdfName(letter1);
        String result2 = FileNameHelper.generatePdfName(letter2);

        // then
        assertThat(result1).isNotEqualTo(result2);
    }

    @Test
    void should_strip_out_underscores_from_service_name() {
        UUID letterId = UUID.randomUUID();
        Letter letter = createLetter(letterId, "typeA", "cmc_claim_store");

        String result = FileNameHelper.generatePdfName(letter);

        assertThat(result).isEqualTo("typeA_cmcclaimstore_" + letterId + ".pdf");
    }

    @Test
    void should_extract_letter_id_from_file_name() {
        asList(
            createLetter(UUID.randomUUID(), "type", "cmc"),
            createLetter(UUID.randomUUID(), "smoke_test", "cmc"),
            createLetter(UUID.randomUUID(), "type", "my_service_"),
            createLetter(UUID.randomUUID(), "some_type", "my_service")
        ).forEach(letter -> {
            String name = FileNameHelper.generatePdfName(letter);
            UUID extractedId = FileNameHelper.extractIdFromPdfName(name);

            assertThat(extractedId).isEqualTo(letter.getId());
        });
    }

    @Test
    void should_throw_custom_exception_when_id_cannot_be_extracted_from_file_name() {
        assertThatThrownBy(
            () -> FileNameHelper.extractIdFromPdfName("a_b.pdf")
        ).isInstanceOf(UnableToExtractIdFromFileNameException.class);
    }

    @Test
    void should_throw_custom_exception_when_uuid_invalid() {
        assertThatThrownBy(
            () -> FileNameHelper.extractIdFromPdfName("a_b_notauuid.pdf")
        ).isInstanceOf(UnableToExtractIdFromFileNameException.class);
    }

    private Letter createLetter(UUID id, String type, String service) {
        Letter result = mock(Letter.class);
        when(result.getId()).thenReturn(id);
        when(result.getType()).thenReturn(type);
        when(result.getService()).thenReturn(service);
        return result;
    }
}
