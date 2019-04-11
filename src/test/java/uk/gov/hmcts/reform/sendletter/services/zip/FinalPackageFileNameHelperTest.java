package uk.gov.hmcts.reform.sendletter.services.zip;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.services.util.FinalPackageFileNameHelper;

import java.time.LocalDateTime;
import java.util.UUID;

import static java.time.LocalDateTime.now;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

class FinalPackageFileNameHelperTest {

    @Test
    void should_generate_expected_file_name() {
        // given
        Letter letter = new Letter(
            randomUUID(),
            randomUUID().toString(),
            "cmc",
            null,
            "type",
            null,
            false,
            now()
        );

        // when
        String name = FinalPackageFileNameHelper.generateName(letter);

        // then
        assertThat(name).isEqualTo(
            "type_cmc_"
                + letter.getCreatedAt().format(FinalPackageFileNameHelper.dateTimeFormatter)
                + "_"
                + letter.getId()
                + ".zip"
        );
    }

    @Test
    void should_generate_expected_file_name_with_explicit_parameters_as_input() {
        UUID letterId = randomUUID();
        LocalDateTime created = now();

        String name = FinalPackageFileNameHelper.generateName("type", "cmc", created, letterId,true);

        assertThat(name).isEqualTo(
            "type_cmc_"
                + created.format(FinalPackageFileNameHelper.dateTimeFormatter)
                + "_"
                + letterId
                + ".pgp"
        );
    }

    @Test
    void should_remove_underscores_from_service_name() {
        // given
        Letter letter = new Letter(
            randomUUID(),
            randomUUID().toString(),
            "cmc_claim_store",
            null,
            "type",
            null,
            false,
            now()
        );

        // when
        String name = FinalPackageFileNameHelper.generateName(letter);

        // then
        assertThat(name).contains("cmcclaimstore");
    }

    @Test
    void should_remove_underscores_from_letter_type() {
        // given
        Letter letter = new Letter(
            randomUUID(),
            randomUUID().toString(),
            "service",
            null,
            "some_type",
            null,
            false,
            now()
        );

        // when
        String name = FinalPackageFileNameHelper.generateName(letter);

        // then
        assertThat(name).contains("sometype");
    }

    @Test
    void should_set_file_extension_based_on_whether_letter_is_encrypted() {
        Letter zippedLetter = new Letter(
            randomUUID(),
            randomUUID().toString(),
            "cmc",
            null,
            "type",
            null,
            false,
            now()
        );

        Letter encryptedLetter = new Letter(
            randomUUID(),
            randomUUID().toString(),
            "cmc",
            null,
            "type",
            null,
            true,
            now()
        );

        assertThat(FinalPackageFileNameHelper.generateName(zippedLetter)).endsWith(".zip");
        assertThat(FinalPackageFileNameHelper.generateName(encryptedLetter)).endsWith(".pgp");
    }
}
