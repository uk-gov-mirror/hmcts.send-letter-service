package uk.gov.hmcts.reform.sendletter.services.zip;

import org.junit.Test;
import uk.gov.hmcts.reform.sendletter.entity.Letter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;

public class ZipFileNameHelperTest {

    @Test
    public void should_generate_expected_file_name() {
        // given
        Letter letter = new Letter(randomUUID(), randomUUID().toString(), "cmc", null, "type", null);
        LocalDateTime timestamp =
            LocalDateTime.of(
                LocalDate.of(2018, 3, 22),
                LocalTime.of(16, 22, 11)
            );

        // when
        String name = ZipFileNameHelper.generateName(letter, timestamp);

        // then
        assertThat(name).isEqualTo("type_cmc_22032018162211_" + letter.getId() + ".zip");
    }

    @Test
    public void should_remove_underscores_from_service_name() {
        // given
        Letter letter = new Letter(randomUUID(), randomUUID().toString(), "cmc_claim_store", null, "type", null);

        // when
        String name = ZipFileNameHelper.generateName(letter, LocalDateTime.now());

        // then
        assertThat(name).contains("cmcclaimstore");
    }
}
