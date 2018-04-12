package uk.gov.hmcts.reform.sendletter.services.zip;

import org.junit.Test;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.services.util.FinalPackageFileNameHelper;

import java.time.LocalDateTime;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

public class FinalPackageFileNameHelperTest {

    @Test
    public void should_generate_expected_file_name() {
        // given
        Letter letter = new Letter(randomUUID(), randomUUID().toString(), "cmc", null, "type", null, false);
        LocalDateTime createdAt = letter.getCreatedAt().toLocalDateTime();

        // when
        String name = FinalPackageFileNameHelper.generateName(letter);

        // then
        assertThat(name).isEqualTo(
            "type_cmc_"
                + createdAt.format(FinalPackageFileNameHelper.dateTimeFormatter)
                + "_"
                + letter.getId()
                + ".zip"
        );
    }

    @Test
    public void should_remove_underscores_from_service_name() {
        // given
        Letter letter = new Letter(randomUUID(), randomUUID().toString(), "cmc_claim_store", null, "type", null, false);

        // when
        String name = FinalPackageFileNameHelper.generateName(letter);

        // then
        assertThat(name).contains("cmcclaimstore");
    }

    @Test
    public void should_set_file_extension_based_on_whether_letter_is_encrypted() {
        Letter zippedLetter = new Letter(randomUUID(), randomUUID().toString(), "cmc", null, "type", null, false);
        Letter encryptedLetter = new Letter(randomUUID(), randomUUID().toString(), "cmc", null, "type", null, true);

        assertThat(FinalPackageFileNameHelper.generateName(zippedLetter)).endsWith(".zip");
        assertThat(FinalPackageFileNameHelper.generateName(encryptedLetter)).endsWith(".pgp");
    }
}
