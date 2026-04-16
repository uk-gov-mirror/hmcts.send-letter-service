package uk.gov.hmcts.reform.sendletter.controllers.reports;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.sendletter.exception.CsvReportGenerationException;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;
import uk.gov.hmcts.reform.sendletter.model.out.MissingReportsResponse;
import uk.gov.hmcts.reform.sendletter.model.out.SendLetterResponse;
import uk.gov.hmcts.reform.sendletter.services.ReportsService;
import uk.gov.hmcts.reform.sendletter.util.CsvWriter;

import java.io.File;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

/**
 * Controller for reports.
 */
@RestController
@RequestMapping(path = "/reports")
public class ReportsController {

    private final ReportsService reportsService;

    /**
     * Constructor for the ReportsController.
     * @param reportsService The reports service
     */
    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    /**
     * Retrieves uploaded letters count summary report.
     * @param date The date
     * @return The csv report
     */
    @GetMapping(path = "/count-summary", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(description = "Retrieves uploaded letters count summary report")
    @ApiResponses({
        @ApiResponse(responseCode = "200",
            content = @Content(schema = @Schema(implementation = SendLetterResponse.class)),
            description = "Successfully generated csv report"),
        @ApiResponse(responseCode = "500", description = "Error occurred while generating csv report")
    })
    public ResponseEntity<byte[]> getCountSummary(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        try {
            List<LettersCountSummary> countSummary = reportsService.getCountFor(date);
            File csvFile = CsvWriter.writeLettersCountSummaryToCsv(countSummary);
            return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=letters-count-summary.csv")
                .body(Files.readAllBytes(csvFile.toPath()));
        } catch (Exception e) {
            throw new CsvReportGenerationException(e);
        }
    }


    /**
     * Checks if all expected reports for a date range are present.
     *
     * @param startDate The start date of the range to check
     * @param endDate   The end date of the range to check
     * @return ResponseEntity with HTTP 200 if all reports are present, or HTTP 404 with list of missing reports
     */
    @GetMapping(path = "/check-reports")
    @Operation(description = "Checks if all reports for a date range are present")
    public ResponseEntity<List<MissingReportsResponse>> checkReports(
        @RequestParam(name = "startDate") @DateTimeFormat(iso = DATE) LocalDate startDate,
        @RequestParam(name = "endDate") @DateTimeFormat(iso = DATE) LocalDate endDate
    ) {
        List<MissingReportsResponse> missingReports = reportsService.checkReports(startDate, endDate);

        if (missingReports.isEmpty()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(missingReports);
        }
    }

}
