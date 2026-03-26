package uk.gov.hmcts.reform.sendletter.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.config.ReportsServiceConfig;
import uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo;
import uk.gov.hmcts.reform.sendletter.entity.LetterStatus;
import uk.gov.hmcts.reform.sendletter.entity.ReportRepository;
import uk.gov.hmcts.reform.sendletter.exception.LetterNotFoundException;
import uk.gov.hmcts.reform.sendletter.model.out.CheckPostedTaskResponse;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckLettersPostedService {

    private static final String TASK_NAME = "Check letters posted";

    @Value("${stale-letters.min-age-in-days-for-no-report-abort}")
    private int minAgeInDaysForNoReportAbort;
    private final StaleLetterService staleLetterService;
    private final LetterService letterService;
    private final LetterActionService letterActionService;
    private final ReportRepository reportRepository;
    private final ReportsServiceConfig reportsServiceConfig;

    /**
     * This function performs the following tasks.
     *
     * <ol>
     *     <li>Retrieves the set of documents that have been in an
     *         {@link LetterStatus#Uploaded} status for more than 7 days.</li>
     *     <li>For each letter retrieved:
     *         <ol>
     *             <li>Works out the expected report code, date and international status using the status/attributes
     *                 of the letter.</li>
     *             <li>If a report was NOT recorded for that date (no row in reports table): Sets the letter to
     *                 {@link LetterStatus#NoReportAborted}.</li>
     *         </ol>
     *     </li>
     * </ol>
     * @return a{@link CheckPostedTaskResponse} that contains details of the letters marked as
     *         {@link LetterStatus#NoReportAborted}.
     */
    public CheckPostedTaskResponse checkLetters() {
        log.info("Started '{}' task", TASK_NAME);
        int count = 0;
        List<BasicLetterInfo> letters = staleLetterService.getStaleLettersWithValidPrintDate(
            List.of(LetterStatus.Uploaded),
            LocalDateTime.now(ZoneOffset.UTC).minusDays(minAgeInDaysForNoReportAbort)
        );
        for (BasicLetterInfo letter : letters) {
            try {
                if (!reportExistsForLetter(letter)) {
                    count += letterActionService.markLetterAsNoReportAborted(letter.getId());
                }
            } catch (LetterNotFoundException e) {
                log.warn("Letter not found for id {} during posted check", letter.getId());
            }
        }
        log.info("Completed '{}' task", TASK_NAME);
        return new CheckPostedTaskResponse(count);
    }

    private boolean reportExistsForLetter(BasicLetterInfo letter) {
        uk.gov.hmcts.reform.sendletter.model.out.LetterStatus status =
            letterService.getStatus(letter.getId(), Boolean.TRUE.toString(), Boolean.FALSE.toString());
        String reportCode = reportsServiceConfig.getReportCode(letter.getService(), status);
        if (reportCode != null) {
            return reportRepository.findFirstByReportCodeAndReportDateAndIsInternational(
                reportCode,
                letter.getSentToPrintAt().toLocalDate(),
                calculateIsInternational(status)
            ).isPresent();
        }
        log.warn("Unable to determine report code for service {}, assuming no report.", letter.getService());
        return false;
    }

    private boolean calculateIsInternational(final uk.gov.hmcts.reform.sendletter.model.out.LetterStatus status) {
        return Optional.ofNullable(status.additionalData)
            .map(m -> m.get("isInternational"))
            .map(Object::toString) // probably unnecessary
            .map(Boolean::valueOf)
            .orElse(false);
    }

}
