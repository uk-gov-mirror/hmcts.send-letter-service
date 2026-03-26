package uk.gov.hmcts.reform.sendletter.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sendletter.model.out.LetterStatus;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = ReportsServiceConfig.class)
@TestPropertySource(properties = """
          reports.service-config[0].service=cmc_claim_store
          reports.service-config[0].display-name=CMC
          reports.service-config[0].report-code=CMC
          reports.service-config[1].service=civil_general_applications
          reports.service-config[1].display-name=general_applications
          reports.service-config[1].report-code=CMC
          reports.service-config[2].service=probate_backend
          reports.service-config[2].display-name=Probate
          reports.service-config[2].report-code=Probate
          reports.service-config[3].service=finrem_document_generator
          reports.service-config[3].display-name=FINREM
          reports.service-config[3].report-code=FRM
          reports.service-config[4].service=fpl_case_service
          reports.service-config[4].display-name=FINREM
          reports.service-config[4].report-code=FRM
          reports.service-config[5].service=sscs
          reports.service-config[5].display-name=SSCS
          reports.service-config[5].report-code=\
    """ + ReportsServiceConfig.SSCS_CODE)
class ReportsServiceConfigTest {

    @Autowired
    private ReportsServiceConfig reportsServiceConfig;

    @Test
    void ensureSscsSpecialCaseForIBAndReform() {
        Set<String> reportCodes = reportsServiceConfig.getReportCodes();
        // SSCS will be returned as two codes (SSCS-IB and SSCS-REFORM)
        assertThat(reportCodes)
            .isNotNull()
            .hasSize(5)
            .contains(ReportsServiceConfig.SSCS_CODE + ReportsServiceConfig.SSCS_IB_SUFFIX)
            .contains(ReportsServiceConfig.SSCS_CODE + ReportsServiceConfig.SSCS_REFORM_SUFFIX);
    }

    @Test
    void shouldReturnCompleteDisplayNameMap() {
        Map<String, String> displayNames = reportsServiceConfig.getServiceDisplayNameMap();
        assertThat(displayNames)
            .isNotNull()
            .hasSize(6)
            .containsEntry("cmc_claim_store", "CMC")
            .containsEntry("civil_general_applications", "general_applications")
            .containsEntry("probate_backend", "Probate")
            .containsEntry("finrem_document_generator", "FINREM")
            .containsEntry("fpl_case_service", "FINREM")
            .containsEntry("sscs", "SSCS");
    }

    @Test
    void shouldReturnCorrectDisplayNameForSpecificService() {
        Map<String, String> displayNames = reportsServiceConfig.getServiceDisplayNameMap();
        for (Map.Entry<String, String> entry : displayNames.entrySet()) {
            assertThat(reportsServiceConfig.getDisplayName(entry.getKey()))
                .isNotNull().isNotEmpty().get().isEqualTo(entry.getValue());
        }
    }

    @Test
    void shouldReturnCorrectReportCodeForSpecificService() {
        final ZonedDateTime now = ZonedDateTime.now();
        final LetterStatus letterStatusNoAttr = new LetterStatus(UUID.randomUUID(), "", "", now, now, now, null, 1);
        assertThat(reportsServiceConfig.getReportCode("cmc_claim_store", letterStatusNoAttr))
            .isNotNull().isEqualTo("CMC");
    }

    @Test
    void shouldReturnCorrectReportCodeWithNullLetterStatus() {
        assertThat(reportsServiceConfig.getReportCode("probate_backend", null))
            .isNotNull().isEqualTo("Probate");
    }

    @Test
    void shouldReturnSscsReformCodeForNonIBService() {
        final ZonedDateTime now = ZonedDateTime.now();
        final UUID id = UUID.randomUUID();
        final Map<String, Object> ibAttr = Map.of("isIbca", true);
        final Map<String, Object> notIbAttr = Map.of("isIbca", false);
        final LetterStatus letterStatusNoAttr = new LetterStatus(id, "", "", now, now, now, null, 1);
        final LetterStatus letterStatusIb = new LetterStatus(id, "", "", now, now, now, ibAttr, 1);
        final LetterStatus letterStatusNoIb = new LetterStatus(id, "", "", now, now, now, notIbAttr, 1);

        // missing isIcba attribute == REFORM
        assertThat(reportsServiceConfig.getReportCode("sscs", letterStatusNoAttr))
            .isNotNull().isEqualTo(ReportsServiceConfig.SSCS_CODE + ReportsServiceConfig.SSCS_REFORM_SUFFIX);

        // isIcba attribute present and set to true == IB
        assertThat(reportsServiceConfig.getReportCode("sscs", letterStatusIb))
            .isNotNull().isEqualTo(ReportsServiceConfig.SSCS_CODE + ReportsServiceConfig.SSCS_IB_SUFFIX);

        // isIcba attribute present and set to false == REFORM
        assertThat(reportsServiceConfig.getReportCode("sscs", letterStatusNoIb))
            .isNotNull().isEqualTo(ReportsServiceConfig.SSCS_CODE + ReportsServiceConfig.SSCS_REFORM_SUFFIX);

        // no letter status provided == REFORM
        assertThat(reportsServiceConfig.getReportCode("sscs", null))
            .isNotNull().isEqualTo(ReportsServiceConfig.SSCS_CODE + ReportsServiceConfig.SSCS_REFORM_SUFFIX);
    }
}
