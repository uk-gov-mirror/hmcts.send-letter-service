package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.config.ReportsServiceConfig;
import uk.gov.hmcts.reform.sendletter.entity.LettersCountSummaryRepository;
import uk.gov.hmcts.reform.sendletter.entity.reports.ServiceLettersCountSummary;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sendletter.util.TimeZones.localDateTimeWithUtc;

@Service
public class ReportsService {

    private static final String TEST_SERVICE = "Bulk Print";

    private final LettersCountSummaryRepository repo;

    private final ReportsServiceConfig reportsServiceConfig;

    private final ZeroRowFiller zeroRowFiller;

    private final String timeFromHour;

    private final String timeToHour;

    public ReportsService(
        LettersCountSummaryRepository repo,
        ReportsServiceConfig reportsServiceConfig,
        ZeroRowFiller zeroRowFiller,
        @Value("${ftp.downtime.from}") String downtimeFromHour,
        @Value("${ftp.downtime.to}") String downtimeToHour
    ) {
        this.repo = repo;
        this.reportsServiceConfig = reportsServiceConfig;
        this.zeroRowFiller = zeroRowFiller;
        this.timeFromHour = downtimeToHour;
        this.timeToHour = downtimeFromHour;
    }

    public List<LettersCountSummary> getCountFor(LocalDate date) {
        LocalDateTime dateTimeFrom = localDateTimeWithUtc(date.minusDays(1), LocalTime.parse(timeFromHour));
        LocalDateTime dateTimeTo = localDateTimeWithUtc(date, LocalTime.parse(timeToHour));

        return zeroRowFiller.fill(
            repo.countByDate(dateTimeFrom, dateTimeTo).map(this::fromDb).collect(toList()))
            .stream()
            .filter(
                summary -> isNotBlank(summary.serviceName) && !summary.serviceName.equals(TEST_SERVICE)
            ) //excludes nulls, empty values and test service
            .collect(toList());
    }

    private LettersCountSummary fromDb(ServiceLettersCountSummary dbSummary) {
        return new LettersCountSummary(
            reportsServiceConfig.getServiceConfig().get(dbSummary.getService()), //get service's display name
            dbSummary.getUploaded());
    }
}
