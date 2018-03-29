package uk.gov.hmcts.reform.sendletter.data.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
public class V006MigrationScriptTest {

    private static final String BASE_VERSION = "005";
    private static final String TARGET_VERSION = "006";

    @Autowired
    private Flyway flyway;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        flyway.clean();
        migrateToVersion(BASE_VERSION);
    }

    @Test
    public void should_set_status_to_uploaded_when_letter_not_printed() throws Exception {
        UUID letterId = insertLetterWithPrintedAt(null);
        migrateToVersion(TARGET_VERSION);
        assertThat(getStatusForLetter(letterId)).isEqualTo("Uploaded");
    }

    @Test
    public void should_set_status_to_posted_when_letter_printed() throws Exception {
        UUID letterId = insertLetterWithPrintedAt(LocalDate.now());
        migrateToVersion(TARGET_VERSION);
        assertThat(getStatusForLetter(letterId)).isEqualTo("Posted");
    }

    private void migrateToVersion(String version) {
        flyway.setTarget(MigrationVersion.fromVersion(version));
        flyway.migrate();
    }

    private UUID insertLetterWithPrintedAt(LocalDate printedAt) {
        UUID letterId = UUID.randomUUID();

        jdbcTemplate.update(
            "INSERT INTO letters "
                + "(id, message_id, service, type, created_at, sent_to_print_at, printed_at, additional_data) "
                + "VALUES "
                + "(:id, :messageId, :service, :type, :createdAt, :sentToPrintAt, :printedAt, :additionalData::JSON)",
            new MapSqlParameterSource()
                .addValue("id", letterId)
                .addValue("messageId", "messageId123")
                .addValue("service", "test-service")
                .addValue("type", "test-letter-type")
                .addValue("createdAt", Date.valueOf(LocalDate.now()))
                .addValue("additionalData", "{}")
                .addValue("sentToPrintAt", null)
                .addValue("printedAt", printedAt != null ? Date.valueOf(printedAt) : null)
        );

        return letterId;
    }

    private String getStatusForLetter(UUID letterId) {
        List<String> results = jdbcTemplate.query(
            "SELECT status FROM letters where id = :id",
            new MapSqlParameterSource().addValue("id", letterId),
            new StatusMapper()
        );

        assertThat(results).hasSize(1);

        return results.get(0);
    }

    private static final class StatusMapper implements RowMapper<String> {
        @Override
        public String mapRow(ResultSet rs, int rowNumber) throws SQLException {
            return rs.getString("status");
        }
    }
}
