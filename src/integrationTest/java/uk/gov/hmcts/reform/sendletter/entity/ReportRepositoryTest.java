package uk.gov.hmcts.reform.sendletter.entity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @BeforeEach
    void setUp() {
        reportRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        reportRepository.deleteAll();
    }

    @Test
    void should_save_and_find_report_by_code_date_and_international_false() {
        Report report = Report.builder()
            .reportName("Test Report")
            .reportCode("CODE1")
            .reportDate(LocalDate.now())
            .printedLettersCount(10)
            .isInternational(false)
            .status(ReportStatus.SUCCESS)
            .build();
        reportRepository.save(report);

        Optional<Report> found = reportRepository.findFirstByReportCodeAndReportDateAndIsInternational(
            "CODE1", LocalDate.now(), false);
        assertThat(found).isPresent();
        assertThat(found.get().getReportName()).isEqualTo("Test Report");
        assertThat(found.get().getPrintedLettersCount()).isEqualTo(10);
        assertThat(found.get().isInternational()).isFalse();
        assertThat(found.get().getStatus()).isEqualTo(ReportStatus.SUCCESS);
    }

    @Test
    void should_return_empty_when_no_matching_report() {
        Optional<Report> found = reportRepository.findFirstByReportCodeAndReportDateAndIsInternational(
            "NON_EXISTENT", LocalDate.now(), false);
        assertThat(found).isNotPresent();
    }

    @Test
    void should_find_reports_processed_after_given_timestamp() {
        LocalDateTime beforeSave = LocalDateTime.now().minusMinutes(1);
        Report report1 = Report.builder()
            .reportName("Report 1")
            .reportCode("CODE1")
            .reportDate(LocalDate.now())
            .printedLettersCount(5)
            .isInternational(false)
            .status(ReportStatus.SUCCESS)
            .build();
        Report report2 = Report.builder()
            .reportName("Report 2")
            .reportCode("CODE2")
            .reportDate(LocalDate.now())
            .printedLettersCount(7)
            .isInternational(true)
            .status(ReportStatus.FAIL)
            .build();
        Report report3 = Report.builder()
            .reportName("Report 3")
            .reportCode("CODE3")
            .reportDate(LocalDate.now())
            .printedLettersCount(9)
            .isInternational(false)
            .status(ReportStatus.SUCCESS)
            .build();
        reportRepository.save(report1);
        reportRepository.save(report2);
        reportRepository.save(report3);
        // All reports should have processedAt after beforeSave
        List<Report> found = reportRepository.findByProcessedAtAfter(beforeSave);
        assertThat(found).hasSize(3);
        assertThat(found).extracting(Report::getReportName)
            .containsExactlyInAnyOrder("Report 1", "Report 2", "Report 3");
    }

    @Test
    void should_return_empty_list_when_no_reports_processed_after_given_timestamp() {
        Report report = Report.builder()
            .reportName("Report 1")
            .reportCode("CODE1")
            .reportDate(LocalDate.now())
            .printedLettersCount(5)
            .isInternational(false)
            .status(ReportStatus.SUCCESS)
            .build();
        reportRepository.save(report);
        // Use a timestamp after the save, so no reports should match
        LocalDateTime afterSave = LocalDateTime.now().plusMinutes(1);
        List<Report> found = reportRepository.findByProcessedAtAfter(afterSave);
        assertThat(found).isEmpty();
    }
}
