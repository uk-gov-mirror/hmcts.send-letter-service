package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.sendletter.entity.reports.ServiceLettersCountSummary;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Stream;

public interface LettersCountSummaryRepository extends JpaRepository<Letter, UUID> {

    @Query(
        nativeQuery = true,
        value = "SELECT COUNT(1) as uploaded, service FROM letters\n"
            + "  WHERE sent_to_print_at >= :dateFrom\n"
            + "    AND sent_to_print_at <=  :dateTo\n"
            + "  GROUP BY service \n"
            + "  ORDER BY service ASC"
    )
    Stream<ServiceLettersCountSummary> countByDate(
        @Param("dateFrom") LocalDateTime dateFrom,
        @Param("dateTo") LocalDateTime dateTo);
}
