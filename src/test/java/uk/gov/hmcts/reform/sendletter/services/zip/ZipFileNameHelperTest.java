package uk.gov.hmcts.reform.sendletter.services.zip;

import org.junit.Test;
import uk.gov.hmcts.reform.sendletter.entity.Letter;

import java.time.LocalDateTime;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

public class ZipFileNameHelperTest {

    @Test
    public void should_generate_expected_file_name() {
        // given
        Letter letter = new Letter(randomUUID(), randomUUID().toString(), "cmc", null, "type", null, false);
        LocalDateTime createdAt = letter.getCreatedAt().toLocalDateTime();

        // when
        String name = ZipFileNameHelper.generateName(letter);

        // then
        assertThat(name).isEqualTo(
            "type_cmc_"
                + createdAt.format(ZipFileNameHelper.dateTimeFormatter)
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
        String name = ZipFileNameHelper.generateName(letter);

        // then
        assertThat(name).contains("cmcclaimstore");
    }
}
