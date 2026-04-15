package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
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

    /**
     * Retrieves all reports within a specified date range.
     *
     * @param startDate the start date of the range
     * @param endDate the end date of the range
     *
     * @return a list of {@link Report} entities
     */
    List<Report> findByReportDateBetween(LocalDate startDate, LocalDate endDate);
}


