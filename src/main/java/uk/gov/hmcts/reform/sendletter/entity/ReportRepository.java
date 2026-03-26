package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, UUID> {

    /**
     * Retrieves the details of the first report received for any given day that matches the
     * input parameters.
     *
     * @param reportCode the report code
     * @param reportDate the report date
     * @param isInternational the international status of the report
     *
     * @return an {@link Optional} {@link Report} entity
     */
    Optional<Report> findFirstByReportCodeAndReportDateAndIsInternational(
        String reportCode, LocalDate reportDate, boolean isInternational);
}


